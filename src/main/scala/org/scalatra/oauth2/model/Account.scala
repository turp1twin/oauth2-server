package org.scalatra
package oauth2
package model

import com.novus.salat._
import com.novus.salat.global._
import com.novus.salat.annotations._
import com.novus.salat.dao._
import org.scalatra.oauth2.UserProvider
import scala.util.control.Exception._
import scalaz._
import Scalaz._
import OAuth2Imports._
import org.mindrot.jbcrypt.BCrypt
import akka.actor.ActorSystem
import databinding.FieldValidation
import org.scalatra.validation.{ FieldName, ValidationFail, ValidationError }
import commands._

case class BCryptPassword(pwd: String, salted: Boolean, stretches: Int) {
  def encrypted = salted ? this | BCryptPassword.hash(this)
  def isMatch(candidate: String) = BCryptPassword.isMatch(candidate, this)
  def matches(candidate: String) = BCryptPassword.matches(candidate, this)
}
object BCryptPassword {
  def apply(in: String, stretches: Int = 10): BCryptPassword = BCryptPassword(in, false, stretches)

  def hash(pwd: BCryptPassword): BCryptPassword =
    createHashed(pwd.pwd, BCrypt.gensalt(pwd.stretches), pwd.stretches)

  def hash(pwd: String, stretches: Int = 10): BCryptPassword =
    createHashed(pwd, BCrypt.gensalt(stretches), stretches)

  private def createHashed(pwd: String, salt: String, stretches: Int) = {
    BCryptPassword(BCrypt.hashpw(pwd, salt), true, stretches)
  }

  def isMatch(candidate: String, toMatch: BCryptPassword): Boolean =
    toMatch.salted ? BCrypt.checkpw(candidate, toMatch.pwd) | isMatch(candidate, toMatch.pwd)

  def isMatch(candidate: String, toMatch: String): Boolean = { candidate == toMatch }

  def matches(candidate: String, toMatch: BCryptPassword): FieldValidation[BCryptPassword] = {
    if (isMatch(candidate, toMatch)) BCryptPassword(candidate).encrypted.success else ValidationError("Passwords don't match.", FieldName("password")).fail
  }

  def random = {
    BCryptPassword.hash(Token.generate(8).token)
  }

  def parse(in: String): Option[BCryptPassword] = in.blankOption map (BCryptPassword(_))
}

case class LinkedOAuthAccount(provider: String, id: String)

case class AuthStats(
    loginFailures: Int = 0,
    loginSuccess: Int = 0,
    lastFailureAt: DateTime = MinDate) {
  def tick(ip: String) =
    copy(
      loginSuccess = (loginSuccess + 1),
      loginFailures = 0,
      lastFailureAt = MinDate)

  def tickFailures =
    copy(
      loginFailures = (loginFailures + 1),
      lastFailureAt = DateTime.now)
}

case class Account(
    login: String,
    email: String,
    name: String,
    password: BCryptPassword,
    @Key("_id") id: ObjectId = new ObjectId,
    confirmation: Token = Token(),
    reset: Token = Token(),
    stats: AuthStats = AuthStats(),
    linkedOAuthAccounts: List[LinkedOAuthAccount] = Nil,
    confirmedAt: DateTime = MinDate,
    resetAt: DateTime = MinDate,
    createdAt: DateTime = DateTime.now,
    updatedAt: DateTime = DateTime.now) extends AppUser[BCryptPassword] {
  val isConfirmed = confirmedAt > MinDate
  val isReset = resetAt > MinDate
  val idString = id.toString
}

class AccountDao(collection: MongoCollection)(implicit system: ActorSystem)
    extends SalatCommandableDao[Account, ObjectId](collection = collection)
    //    with UserProvider[Account]
    //    with RememberMeProvider[Account]
    //    with ForgotPasswordProvider[Account]
    //    with AuthenticatedChangePasswordProvider[Account]
    with AccountModelCommands {

  private[this] val oauth = OAuth2Extension(system)
  collection.ensureIndex(Map("login" -> 1, "email" -> 1), "login_email_idx", true)
  collection.ensureIndex(Map("login" -> 1), "login_idx", true)
  collection.ensureIndex(Map("email" -> 1), "email_idx", true)
  collection.ensureIndex(Map("confirmation.token" -> 1), "confirmation_token_idx")
  collection.ensureIndex(Map("reset.token" -> 1), "reset_token_idx")
  collection.ensureIndex(Map("linkedOAuthAccounts.provider" -> 1, "linkedOAuthAccounts.id" -> 1), "linked_oauth_accounts_idx", true)

  def login(command: LoginCommand): ModelValidation[Account] = {
    if (command.isValid) {
      val o = command.retrieved.liftFailNel.map(loggedIn(_, command.ipAddress))
      if (o.isSuccess && o.exists(_.isConfirmed)) o else loginFailed(command)
    } else {
      loginFailed(command)
    }
  }

  private def loginFailed(command: LoginCommand) = {
    allCatch {
      command.retrieved foreach { u ⇒ save(u.copy(stats = u.stats.tickFailures)) }
    }
    ValidationError("Login/password don't match.", ValidationFail).failNel[Account]
  }

  def loggedIn(owner: Account, ipAddress: String): Account = {
    // This ticks the login counter and changes the reset token because if we get here
    // the user clearly remembered the password.
    val ticked = owner.copy(stats = owner.stats.tick(ipAddress), reset = Token())
    save(ticked)
    ticked
  }

  def findByLoginOrEmail(loginOrEmail: String): Option[Account] =
    findOne($or(fieldNames.login -> loginOrEmail, fieldNames.email -> loginOrEmail))

  def findUserById(id: String) = findOneById(new ObjectId(id))

  def findByLinkedAccount(provider: String, id: String) = findOne(Map("linkedOAuthAccounts.provider" -> provider, "linkedOAuthAccounts.id" -> id))

  object validations {
    import org.scalatra.databinding._
    import org.scalatra.validation.Validation._
    import Validations._

    def name(name: String): FieldValidation[String] = nonEmptyString(fieldNames.name, name)

    def login(login: String, id: Option[ObjectId] = None): FieldValidation[String] = {
      for {
        a ← nonEmptyString(fieldNames.login, login)
        b ← minLength(fieldNames.login, a, 3)
        c ← validFormat(fieldNames.login, b, """^\w+([\.\w]*)*$""".r, "%s can only contain letters, numbers, underscores and dots.")
        d ← uniqueField[String](fieldNames.login, c, collection, id)
      } yield d
    }

    def email(email: String, id: Option[ObjectId] = None): FieldValidation[String] =
      for {
        a ← nonEmptyString(fieldNames.email, email)
        b ← validEmail(fieldNames.email, a)
        c ← uniqueField[String](fieldNames.email, b, collection, id)
      } yield c

    def password(password: String): FieldValidation[String] =
      for {
        a ← nonEmptyString(fieldNames.password, password)
        b ← minLength(fieldNames.password, a, 6)
      } yield b

    def passwordWithConfirmation(password: String, passwordConfirmation: String): FieldValidation[String] =
      for {
        a ← this.password(password)
        b ← validConfirmation(fieldNames.password, a, fieldNames.passwordConfirmation, passwordConfirmation)
      } yield b

    def tokenRequired(tokenType: String, token: String): FieldValidation[String] =
      nonEmptyString(tokenType.toLowerCase + "." + fieldNames.token, token)

    def validPassword(owner: Account, password: String): FieldValidation[Account] =
      if (owner.password.isMatch(password)) owner.success
      else ValidationError("The username/password combination doesn not match", ValidationFail).fail

    /*_*/
    def apply(owner: Account): ModelValidation[Account] = {
      val factory: Factory = owner.copy(_, _, _)
      (login(owner.login, owner.id.some).liftFailNel
        |@| email(owner.email, owner.id.some).liftFailNel
        |@| name(owner.name).liftFailNel)(factory)
    }
    /*_*/
  }

  def register(cmd: RegisterCommand): ModelValidation[Account] = {
    val res = execute(cmd)
    if (!oauth.isTest && cmd.isValid)
      res foreach { o ⇒
        oauth.smtp.send(MailMessage(ConfirmationMail(o.name, o.login, o.email, o.confirmation.token)))
      }
    res
  }

  private type Factory = (String, String, String) ⇒ Account

  def validate(user: Account): ModelValidation[Account] = validations(user)

  def confirm(cmd: ActivateAccountCommand): ModelValidation[Account] = {
    val key = fieldNames.confirmation + "." + fieldNames.token
    cmd.token.validation.liftFailNel flatMap { tok ⇒
      findOne(Map(key -> tok)) map { owner ⇒
        if (!owner.isConfirmed) {
          val upd = owner.copy(confirmedAt = DateTime.now)
          save(upd)
          upd.successNel[ValidationError]
        } else OAuth2Error.AlreadyConfirmed.failNel
      } getOrElse OAuth2Error.InvalidToken.failNel
    }
  }

  def forgot(forgotCommand: ForgotCommand): ModelValidation[Account] = {
    forgotCommand.login.validation.liftFailNel flatMap { loe ⇒
      findByLoginOrEmail(loe) map { owner ⇒
        val updated = owner.copy(reset = Token(), resetAt = MinDate)
        save(updated)
        if (!oauth.isTest) oauth.smtp.send(MailMessage(SendForgotPasswordMail(updated.name, updated.login, updated.email, updated.reset.token)))
        updated.successNel
      } getOrElse ValidationError("Account not found.", org.scalatra.validation.NotFound).failNel
    }
  }

  /*_*/
  def resetPassword(command: ResetCommand): ModelValidation[Account] = {
    if (command.isValid) {
      doReset(~command.token.value, ~command.password.value)
    } else ValidationError("Password reset failed.").failNel
  }
  /*_*/

  def changePassword(command: ChangePasswordCommand): ModelValidation[Account] = {
    if (command.isValid) {
      val u = command.user.copy(password = BCryptPassword(~command.password.value).encrypted, resetAt = DateTime.now, reset = Token())
      save(u)
      u.successNel
    } else ValidationError("Changing password failed.", org.scalatra.validation.ValidationFail).failNel
  }

  private def doReset(token: String, password: String): ModelValidation[Account] = {
    val key = fieldNames.reset + "." + fieldNames.token
    findOne(Map(key -> token)) map { owner ⇒
      owner.isReset ? OAuth2Error.InvalidToken.failNel[Account] | {
        val upd = owner.copy(password = BCryptPassword(password).encrypted, resetAt = DateTime.now, reset = Token())
        save(upd)
        upd.successNel[ValidationError]
      }
    } getOrElse OAuth2Error.InvalidToken.failNel[Account]
  }

  override def save(t: Account, wc: WriteConcern) {
    super.save(t.copy(updatedAt = DateTime.now), wc)
  }

}


import akka.actor.ActorSystem
import io.backchat.oauth2.{ OAuthAuthentication, ClientsCrudApp, HomeServlet, OAuth2Extension }
import javax.servlet.ServletContext
import org.scalatra.LifeCycle
import akka.util.duration._

class Scalatra extends LifeCycle {

  implicit var system: ActorSystem = null
  override def init(context: ServletContext) {

    system = context.getOrElseUpdate(io.backchat.oauth2.ActorSystemContextKey, ActorSystem(io.backchat.oauth2.ActorSystemName)).asInstanceOf[ActorSystem]
    val oauth = OAuth2Extension(system)

    context mount (new HomeServlet, "/")
    context mount (new ClientsCrudApp, "/clients")
    context mount (new OAuthAuthentication, "/auth")
  }

  override def destroy(context: ServletContext) {
    system.synchronized {
      if (system != null && !system.isTerminated) {
        system.shutdown()
        system.awaitTermination(30 seconds)
      }
    }
  }
}
import scala.xml.Group
import com.typesafe.startscript.StartScriptPlugin
import scalariform.formatter.preferences._
import UniqueVersionKeys._ // put this at the top
//import RequireJsKeys._
import StartScriptPlugin._
import ScalateKeys._
import Wro4jKeys._
import net.liftweb.json._
import JsonDSL._

organization := "org.scalatra.oauth2"

name := "oauth2-server"

version := "0.2.0-SNAPSHOT"

scalaVersion := "2.9.2"

//javacOptions ++= Seq("-Xlint:unchecked", "-source", "1.7", "-target", "1.7", "-Xlint:deprecation")

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

scalacOptions ++= Seq("-optimize", "-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-P:continuations:enable")

autoCompilerPlugins := true

libraryDependencies ++= Seq(
  compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.2"),
  compilerPlugin("org.scala-tools.sxr" % "sxr_2.9.0" % "0.2.7")
)

ivyXML :=
  <dependencies>
    <exclude org="org.eclipse.jetty.orbit" />    
  </dependencies>

seq(webSettings:_*)

libraryDependencies ++= Seq(
  "org.scalatra"            % "scalatra"               % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "scalatra-auth"          % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "scalatra-scalate"       % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "scalatra-lift-json"     % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "scalatra-swagger"       % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "scalatra-slf4j"         % "2.1.0-RC3" intransitive(),
  "org.scalatra"            % "contrib-commons"        % "1.0.5-RC3" intransitive(),
  "org.scalatra"            % "contrib-validation"     % "1.0.5-RC3" intransitive(),
  "net.liftweb"             % "lift-json_2.9.1"        % "2.4" exclude("org.scala-lang", "scalap"),
  "net.liftweb"             % "lift-json-scalaz_2.9.1" % "2.4" intransitive(),
  "net.liftweb"             % "lift-json-ext_2.9.1"    % "2.4" intransitive(),
  "org.scalaz"             %% "scalaz"                 % "6.0.4",
  "org.mozilla"             % "rhino"                  % "1.7R4",
  "org.jruby"               % "jruby"                  % "1.6.7.2",
  "net.databinder.dispatch" % "core_2.9.2"             % "0.9.0",
  "org.clapper"             % "scalasti_2.9.1"         % "0.5.8",
  "org.mindrot"             % "jbcrypt"                % "0.3m",
  "org.scribe"              % "scribe"                 % "1.3.1",
  "javax.mail"              % "mail"                   % "1.4.5",
  "commons-codec"           % "commons-codec"          % "1.6",
//  "ro.isdc.wro4j"           % "wro4j-core"           % "1.4.7" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
//  "ro.isdc.wro4j"           % "wro4j-extensions"     % "1.4.7" exclude("log4j", "log4j") exclude("org.slf4j", "slf4j-log4j12"),
  "com.typesafe.akka"       % "akka-actor"             % "2.0.2",
  "com.typesafe.akka"       % "akka-testkit"           % "2.0.2"               % "test",
  "org.fusesource.scalate"  % "scalate-markdownj"      % "1.5.3",
  "org.scala-tools.time"    % "time_2.9.1"             % "0.5",
  "org.scalatra"            % "scalatra-specs2"        % "2.1.0-RC3"           % "test",
  "junit"                   % "junit"                  % "4.10"                % "test",
  "ch.qos.logback"          % "logback-classic"        % "1.0.6",
  "org.eclipse.jetty"       % "jetty-webapp"           % "8.1.5.v20120716"     % "container",
  "javax.servlet"           % "javax.servlet-api"      % "3.0.1"               % "container;provided",
  "com.novus"              %% "salat"                  % "1.9.0"
)

resolvers += "sonatype oss snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "sonatype oss releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.typesafeResolver

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

testOptions += Tests.Setup( () => System.setProperty("akka.mode", "test") )

testOptions += Tests.Argument(TestFrameworks.Specs2, "console", "junitxml")

testOptions <+= (crossTarget map { ct =>
 Tests.Setup { () => System.setProperty("specs2.junit.outDir", new File(ct, "specs-reports").getAbsolutePath) }
})

seq(jrebelSettings: _*)

jrebel.webLinks <+= (sourceDirectory in Compile)(_ / "webapp")

homepage := Some(url("https://github.com/scalatra/oauth2-server"))

startYear := Some(2010)

licenses := Seq(("MIT", url("https://github.com/scalatra/oauth2-server/raw/HEAD/LICENSE")))

pomExtra <<= (pomExtra, name, description) {(pom, name, desc) => pom ++ Group(
  <scm>
    <connection>scm:git:git://github.com/scalatra/oauth2-server.git</connection>
    <developerConnection>scm:git:git@github.com:scalatra/oauth2-server.git</developerConnection>
    <url>https://github.com/scalatra/oauth2-server</url>
  </scm>
  <developers>
    <developer>
      <id>casualjim</id>
      <name>Ivan Porto Carrero</name>
      <url>http://flanders.co.nz/</url>
    </developer>
  </developers>
)}

packageOptions <+= (name, version, organization) map {
    (title, version, vendor) =>
      Package.ManifestAttributes(
        "Created-By" -> "Simple Build Tool",
        "Built-By" -> System.getProperty("user.name"),
        "Build-Jdk" -> System.getProperty("java.version"),
        "Specification-Title" -> title,
        "Specification-Version" -> version,
        "Specification-Vendor" -> vendor,
        "Implementation-Title" -> title,
        "Implementation-Version" -> version,
        "Implementation-Vendor-Id" -> vendor,
        "Implementation-Vendor" -> vendor
      )
  }

publishMavenStyle := true

publishTo <<= version { (v: String) =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

seq(scalariformSettings: _*)

ScalariformKeys.preferences :=
  (FormattingPreferences()
        setPreference(IndentSpaces, 2)
        setPreference(AlignParameters, false)
        setPreference(AlignSingleLineCaseStatements, true)
        setPreference(DoubleIndentClassDeclaration, true)
        setPreference(RewriteArrowSymbols, true)
        setPreference(PreserveSpaceBeforeArguments, true)
        setPreference(IndentWithTabs, false))

(excludeFilter in ScalariformKeys.format) <<= excludeFilter(_ || "*Spec.scala")

seq(scalateSettings:_*)

scalateTemplateDirectory in Compile <<= (baseDirectory) { _ / "src/main/webapp/WEB-INF" }

scalateImports ++= Seq(
  "import scalaz._",
  "import Scalaz._",
  "import org.scalatra.oauth2._",
  "import OAuth2Imports._",
  "import model._"
)

scalateBindings ++= Seq(
  Binding("flash", "scala.collection.Map[String, Any]", defaultValue = "Map.empty"),
  Binding("session", "javax.servlet.http.HttpSession"),
  Binding("sessionOption", "scala.Option[javax.servlet.http.HttpSession]"),
  Binding("params", "scala.collection.Map[String, String]"),
  Binding("multiParams", "org.scalatra.MultiParams"),
  Binding("userOption", "Option[Account]", defaultValue = "None"),
  Binding("user", "Account", defaultValue = "null"),
  Binding("system", "akka.actor.ActorSystem", isImplicit =  true),
  Binding("isAnonymous", "Boolean", defaultValue = "true"),
  Binding("isAuthenticated", "Boolean", defaultValue = "false"))

seq(buildInfoSettings: _*)

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq[Scoped](name, version, scalaVersion, sbtVersion)

buildInfoPackage := "org.scalatra.oauth2"

seq(startScriptForWarSettings: _*)

startScriptJettyVersion in Compile := "8.1.5.v20120716"

startScriptJettyChecksum in Compile := "83ab4d014f4f5c6e378df783d680d4e2aeff3883"

startScriptJettyURL in Compile <<= (startScriptJettyVersion in Compile) { (version) => "http://download.eclipse.org/jetty/" + version + "/dist/jetty-distribution-" + version + ".zip" }

watchSources <++= (sourceDirectory in Compile) map (d => (d / "webapp" ** "*").get)

seq(wro4jSettings: _*)

compile in Compile <<= (compile in Compile).dependsOn(generateResources in Compile)

(webappResources in Compile) <+= (targetFolder in generateResources in Compile)

outputFolder in (Compile, generateResources) := "assets/"

processorProvider in (Compile, generateResources) := new OAuth2Processors

wroFile in (Compile, generateResources) <<= (baseDirectory)(_ / "project" / "wro.xml")

propertiesFile in (Compile, generateResources) <<= (baseDirectory)(_ / "project" / "wro.properties")

//TaskKey[Seq[File]]("coffee-jade", "Compiles view templates to javascript") <<= (baseDirectory, sourceDirectory in Compile, streams) map { (b, dir, s) =>
//  val bd = dir / "webapp"
//  ((b / "project" / "coffeejade.sh").getAbsolutePath+" "+bd.getAbsolutePath+" templates") ! s.log
//  (bd / "templates" ** "*.js").get
//}
//
//generateResources in Compile <<= (generateResources in Compile).dependsOn(TaskKey[Seq[File]]("coffee-jade"))

resolvers += "scct-repo" at "http://mtkopone.github.com/scct/maven-repo/"

seq(instrumentSettings:_*)


uniqueVersionSettings

uniqueVersion := true
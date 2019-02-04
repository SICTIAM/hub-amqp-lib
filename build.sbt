import Dependencies._
import sbt.Opts.resolver
import sbt.url

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / organization := "fr.sictiam"
ThisBuild / organizationName := "SICTIAM"

lazy val root = (project in file("."))
  .settings(
    name := "hub-amqp-lib",
    homepage := Some(url("http://www.sictiam.fr")),
    pomIncludeRepository := { _ => false },
    licenses := Seq("Apache 2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
    scmInfo := Some(
      ScmInfo(
        url("https://bitbucket.org/hub-sictiam/hub-amqp-lib.git"),
        "scm:git@bitbucket.org:hub-sictiam/hub-amqp-lib.git"
      )
    ),
    developers := List(
      Developer(
        id = "ndelaforge",
        name = "Nicolas Delaforge",
        email = "nicolas.delaforge@mnemotix.com",
        url = url("http://www.mnemotix.com")
      ),
      Developer(
        id = "prlherisson",
        name = "Pierre-René Lherisson",
        email = "pr.lherisson@mnemotix.com",
        url = url("http://www.mnemotix.com")
      ),
      Developer(
        id = "mrogelja",
        name = "Mathieu Rogelja",
        email = "mathieu.rogelja@mnemotix.com",
        url = url("http://www.mnemotix.com")
      )
    ),
    publishArtifact := true,
    publishMavenStyle := true,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    resolvers ++= Seq(
      resolver.mavenLocalFile,
      Resolver.mavenLocal,
      "jitpack" at "https://jitpack.io",
      "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
      "Sonatype (Snapshots)" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Typesafe (Releases)" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      logbackClassic % Test,
      jodaTime,
      typesafeConfig,
      playJson,
      scalaLogging,
      //      rabbitmqClient,
      jenaLibs,
      alpakkaAmqp,
      akkaStreamTestkit % Test
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

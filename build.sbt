import Dependencies._
import sbt.url

ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "0.1.2-SNAPSHOT"
ThisBuild / organization := "fr.sictiam"
ThisBuild / organizationName := "SICTIAM"

lazy val root = (project in file("."))
  .settings(
    name := "hub-amqp-lib",
    homepage := Some(url("http://www.sictiam.fr")),
    pomIncludeRepository := { _ => false },
    licenses := Seq("AGPL 3.0" -> url("https://opensource.org/licenses/AGPL-3.0")),
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
      Resolver.mavenLocal,
      //      Resolver.sonatypeRepo("public"),
      //      Resolver.typesafeRepo("releases"),
      //      Resolver.typesafeIvyRepo("releases"),
      //      Resolver.sbtPluginRepo("releases"),
      //      Resolver.bintrayRepo("owner", "repo"),
      "jitpack" at "https://jitpack.io"
    ),
    libraryDependencies ++= Seq(
      scalaTest % Test,
      logbackClassic % Test,
      jodaTime,
      typesafeConfig,
      playJson,
      scalaLogging,
      jenaLibs,
      alpakkaAmqp,
      akkaSlf4j,
      akkaStreamTestkit % Test
    )
  )

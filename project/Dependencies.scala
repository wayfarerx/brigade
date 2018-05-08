import sbt._

object Dependencies {

  lazy val cats = "org.typelevel" %% "cats-effect" % "0.9"
  lazy val akka = "com.typesafe.akka" %% "akka-actor-typed" % "2.5.12"
  lazy val discord4j = "com.github.austinv11" % "Discord4J" % "2.10.0"
  lazy val circe = "io.circe" %% "circe-core" % "0.9.1"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.9.1"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.9.1"
  lazy val json4sNative = "org.json4s" %% "json4s-native" % "3.5.3"
  lazy val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.195"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val akkaTest = "com.typesafe.akka" %% "akka-testkit-typed" % "2.5.12" % Test
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test


  lazy val jcenter = "jcenter" at "http://jcenter.bintray.com"
  lazy val jitpack = "jitpack.io" at "https://jitpack.io"

}

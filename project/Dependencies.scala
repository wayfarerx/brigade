import sbt._

object Dependencies {

  lazy val akka = "com.typesafe.akka" %% "akka-actor-typed" % "2.5.12"

  lazy val discord4j = "com.discord4j" % "Discord4J" % "2.10.1"

  lazy val circe = "io.circe" %% "circe-core" % "0.9.3"
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.9.3"
  lazy val circeParser = "io.circe" %% "circe-parser" % "0.9.3"
  lazy val circeExtras = "io.circe" %% "circe-generic-extras" % "0.9.3"

  lazy val commonsIO = "commons-io" % "commons-io" % "2.6"

  lazy val awsS3 = "software.amazon.awssdk" % "s3" % "2.0.0-preview-9"
  lazy val awsCloudWatch = "software.amazon.awssdk" % "cloudwatch" % "2.0.0-preview-9"

  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  lazy val akkaTest = "com.typesafe.akka" %% "akka-testkit-typed" % "2.5.12" % Test
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test

}

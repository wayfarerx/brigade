import sbt._

object Dependencies {

  lazy val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.4"
  lazy val discord4j = "com.github.austinv11" % "Discord4J" % "2.8.4"
  lazy val json4sNative = "org.json4s" %% "json4s-native" % "3.5.3"
  lazy val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.195"

  lazy val akkaTest = "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % Test
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1" % Test


  lazy val jcenter = "jcenter" at "http://jcenter.bintray.com"
  lazy val jitpack = "jitpack.io" at "https://jitpack.io"

}

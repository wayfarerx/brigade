import sbt._

object Dependencies {

  lazy val discord4j = "com.github.austinv11" % "Discord4J" % "2.8.4"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"


  lazy val jcenter = "jcenter" at "http://jcenter.bintray.com"
  lazy val jitpack = "jitpack.io" at "https://jitpack.io"

}

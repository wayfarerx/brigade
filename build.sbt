import Dependencies._

lazy val common = Seq(
  organization := "net.wayfarerx",
  scalaVersion := "2.12.1",
  version := "0.1.0-SNAPSHOT"
)

lazy val server = (project in file("server")).
  settings(
    common,
    name := "circumvolve-server",
    libraryDependencies += javacord,
    libraryDependencies += logback,
    libraryDependencies += scalaTest % Test
  )

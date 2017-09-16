import Dependencies._

lazy val common = Seq(

  organization := "net.wayfarerx",
  scalaVersion := "2.12.1",
  version := "0.2.0-SNAPSHOT",

  resolvers += jcenter,
  resolvers += jitpack,

  test in assembly := {}

)

lazy val server = (project in file("server")).
  settings(common,

    name := "circumvolve-server",

    libraryDependencies += akka,
    libraryDependencies += discord4j,
    libraryDependencies += json4sNative,
    libraryDependencies += awsS3,
    libraryDependencies += logback,

    libraryDependencies += akkaTest,
    libraryDependencies += scalaTest,

    mainClass in assembly := Some("net.wayfarerx.circumvolve.main.Circumvolve"),
    assemblyJarName in assembly := "circumvolve.jar"

  )

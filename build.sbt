import Dependencies._

lazy val common = Seq(
  organization := "net.wayfarerx",
  scalaVersion := "2.12.1",
  version := "0.5.0-SNAPSHOT",
  test in assembly := {}
)

lazy val aws = (project in file("aws")).settings(common,
  name := "brigade-aws",
  libraryDependencies ++= Vector(akka, commonsIO, awsS3, awsCloudWatch, scalaTest, akkaTest)
)

lazy val server = (project in file("server")).settings(common,

  name := "brigade-server",

  libraryDependencies += akka,
  libraryDependencies += discord4j,
  libraryDependencies += circe,
  libraryDependencies += circeGeneric,
  libraryDependencies += circeParser,
  libraryDependencies += circeExtras,
  libraryDependencies += commonsIO,
  libraryDependencies += awsS3,
  libraryDependencies += awsCloudWatch,
  libraryDependencies += logback,

  libraryDependencies += akkaTest,
  libraryDependencies += scalaTest,

  mainClass in assembly := Some("net.wayfarerx.brigade.main.Program"),
  assemblyJarName in assembly := "brigade.jar"

).dependsOn(aws)

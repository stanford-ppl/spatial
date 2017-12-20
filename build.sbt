name := "pcc"

version := "1.0"

scalaVersion := "2.12.4"

val scalatestVersion = "3.0.4"
val paradiseVersion = "2.1.0"
val virtualizedVersion = "0.2"

val commonSettings = Seq(
  libraryDependencies ++= Seq(
    "org.virtualized" %% "virtualized" % virtualizedVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test",
    "org.scala-lang.modules"  %% "scala-parser-combinators" % "1.0.4"
  ),

  //paradise
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)
)

publishArtifact := false

lazy val forge: Project = project
  .settings(commonSettings)

lazy val core = project
  .settings(commonSettings)
  .dependsOn(forge)

addCommandAlias("make", "compile")
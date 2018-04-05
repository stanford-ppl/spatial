// Build file for testing individual templates

organization := "edu.berkeley.cs"

version := "3.0-SNAPSHOT"

name := "spatial-app"

scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
// The following are the default development versions, not the "release" versions.
val defaultVersions = Map(
  "chisel3" -> "3.0-SNAPSHOT_2017-10-06",
  "chisel-iotesters" -> "1.1-SNAPSHOT"
  )

libraryDependencies ++= (Seq("chisel3","chisel-iotesters").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) })

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
  Seq(
    base / "templates",
    base / "fringeZynq",
    base / "fringeASIC",
    base / "fringeAWS",
    base / "fringeVCS",
    base / "fringeHW"
  )
}

scalaSource in Test := baseDirectory.value / "tests"

// Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

// Disable parallel execution when running te
//  Running tests in parallel on Jenkins currently fails.
parallelExecution in Test := false


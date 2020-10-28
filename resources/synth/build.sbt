// Build file for testing spatial apps

organization := "edu.stanford.cs.dawn"

version := "1.1"

name := "spatial-app"

scalaVersion     := "2.12.6"

scalacOptions ++= Seq("-explaintypes", "-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls", "-Xsource:2.11")

// Provide a managed dependency on X if -DXVersion="" is supplied on the command line.
// The following are the default development versions, not the "release" versions.
val defaultVersions = Map(
  "chisel3" -> "3.1.6",
  "chisel-iotesters" -> "1.2.8"
  )

libraryDependencies ++= (Seq("chisel3","chisel-iotesters").map {
  dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) })

libraryDependencies ++= Seq(
  "org.typelevel" %% "spire" % "0.16.0",
  "org.scalanlp" %% "breeze" % "0.13.2",
  "org.scalatest" %% "scalatest" % "3.0.8",
  "org.scalacheck" %% "scalacheck" % "1.12.6"
)

val spatialVersion = "1.1-SNAPSHOT-nzhang"

libraryDependencies += "edu.stanford.cs.dawn" %% {"fringe" + sys.env.get("FRINGE_PACKAGE").getOrElse("")} % spatialVersion
libraryDependencies += "edu.stanford.cs.dawn" %% {"models" + sys.env.get("MODELS_PACKAGE").getOrElse("")} % spatialVersion
libraryDependencies += "edu.stanford.cs.dawn" %% {"utils" + sys.env.get("UTILS_PACKAGE").getOrElse("")} % spatialVersion

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// resourceDirectory in Compile := baseDirectory.value / "chisel" / "template-level" / "resources"

scalaSource in Compile := baseDirectory.value / "chisel"

scalaSource in Test := baseDirectory.value / "chisel" / "app-tests"

excludeFilter in (Compile, unmanagedSources) := "*altera*"



// Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

// Disable parallel execution when running te
//  Running tests in parallel on Jenkins currently fails.
parallelExecution in Test := false

val common = Seq(
  name := "RuntimeModel",

  scalaVersion := "2.12.5",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls"),

  scalaSource in Compile := baseDirectory.value,
  scalaSource in Test := baseDirectory.value,

  libraryDependencies += "edu.stanford.cs.dawn" %% {"models" + sys.env.get("MODELS_PACKAGE").getOrElse("")} % "1.1-SNAPSHOT",

  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    Resolver.sonatypeRepo("releases")
  ),

  // Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt 
  logBuffered in Test := false,

  // Disable parallel execution when running te 
  //  Running tests in parallel on Jenkins currently fails. 
  parallelExecution in Test := false
)


lazy val model = project.settings(common)

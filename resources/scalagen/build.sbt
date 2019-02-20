// Build file for testing spatial apps
name := "spatial-app"

scalaVersion := "2.12.5"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

scalaSource in Compile := baseDirectory.value / "scala"
scalaSource in Test := baseDirectory.value / "scala"

libraryDependencies += "edu.stanford.cs.dawn" %% {"emul" + sys.env.get("EMUL_PACKAGE").getOrElse("")} % "1.1-SNAPSHOT"
libraryDependencies += "edu.stanford.cs.dawn" %% {"models" + sys.env.get("MODELS_PACKAGE").getOrElse("")} % "1.1-SNAPSHOT"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("releases")
)

// Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

// Disable parallel execution when running te
//  Running tests in parallel on Jenkins currently fails.
parallelExecution in Test := false

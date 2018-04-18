// Build file for testing spatial apps
name := "spatial-app"

scalaVersion := "2.12.5"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

scalaSource in Compile := baseDirectory.value / "scala"
scalaSource in Test := baseDirectory.value / "scala"

libraryDependencies += "edu.stanford.ppl" %% "emul" % "1.0"

// Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

// Disable parallel execution when running te
//  Running tests in parallel on Jenkins currently fails.
parallelExecution in Test := false

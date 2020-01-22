// Build file for testing spatial apps
name := "pir-app"

scalaVersion := "2.12.10"
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

scalaSource in Compile := baseDirectory.value / "pir"
unmanagedSourceDirectories in Compile += baseDirectory.value / "model"
scalaSource in Test := baseDirectory.value / "pir"

libraryDependencies += "edu.stanford.ppl" %% "pir" % "0.1"
/*libraryDependencies += "edu.stanford.ppl" %% "emul" % "1.0"*/
libraryDependencies += "edu.stanford.cs.dawn" %% {"models" + sys.env.get("MODELS_PACKAGE").getOrElse("")} % "1.1-SNAPSHOT"

// Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
logBuffered in Test := false

// Disable parallel execution when running te
//  Running tests in parallel on Jenkins currently fails.
parallelExecution in Test := false
addCommandAlias("make", "compile")
excludeFilter in unmanagedSources := HiddenFileFilter || "*trace*"

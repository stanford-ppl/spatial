name := "pcl-core"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "com.github.pureconfig" %% "pureconfig" % "0.7.0",
  "org.apache.commons" % "commons-lang3" % "3.3.2",
  "commons-io" % "commons-io" % "2.5"
)

/** Scalac Options **/
scalacOptions += "-Yno-generic-signatures"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")
scalacOptions ++= Seq("-language:higherKinds", "-language:implicitConversions")

parallelExecution in Test := false
concurrentRestrictions in Global += Tags.limitAll(1) // we need tests to run in isolation across all projects
resourceDirectory in Compile :=  baseDirectory(_/ "resources").value


scalaSource in Compile := baseDirectory(_/"src").value
scalaSource in Test := baseDirectory(_/"test").value
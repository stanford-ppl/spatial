name := "forge"

scalaVersion := "2.12.4"

scalacOptions += "-language:experimental.macros"

scalacOptions += "-Yno-generic-signatures"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings")
scalacOptions ++= Seq("-language:higherKinds", "-language:implicitConversions")


scalaSource in Compile := baseDirectory(_/ "src").value
scalaSource in Test := baseDirectory(_/"test").value

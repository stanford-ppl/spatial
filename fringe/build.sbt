val fringe_version    = "1.0"
val scala_version     = "2.11.7"
val paradise_version  = "2.1.0"
val scalatest_version = "3.0.5"
val chisel3_version   = sys.props.getOrElse("chisel3Version", "3.0-SNAPSHOT_2017-10-06")
val testers_version   = sys.props.getOrElse("chisel-iotestersVersion", "1.1-SNAPSHOT")

name := "fringe" + sys.env.get("FRINGE_PACKAGE").getOrElse("")
scalaVersion := scala_version
version := fringe_version
organization := "edu.stanford.ppl"

val settings = Seq(
  /** External Libraries (e.g. maven dependencies) **/
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scala_version,            // Reflection
    "org.scalatest" %% "scalatest" % scalatest_version,	           // Testing
    "edu.berkeley.cs" %% "chisel3" % chisel3_version,              // Chisel
    "edu.berkeley.cs" %% "chisel-iotesters" % testers_version,     // Chisel testing
    "edu.stanford.ppl" %% "emul" % "1.0"                           // Numeric emulation
  ),

  /** Scalac Options **/
  scalacOptions += "-target:jvm-1.8",               // JVM 1.8
  scalacOptions ++= Seq("-encoding", "UTF-8"),      // Encoding using UTF-8
  scalacOptions += "-unchecked",                    // Enable additional warnings
  scalacOptions += "-deprecation",                  // Enable warnings on deprecated usage
  scalacOptions += "-feature",                      // Warnings for features requiring explicit import
  scalacOptions += "-Xfatal-warnings",              // Warnings are errors
  scalacOptions += "-language:higherKinds",         // Globally enable higher kinded type parameters
  scalacOptions += "-language:implicitConversions", // Globally enable implicit conversions
  scalacOptions += "-language:reflectiveCalls",     // Globally enable reflective calls
  scalacOptions += "-language:experimental.macros", // Globally enable macros
  scalacOptions += "-language:existentials",        // Globally enable existentials
  scalacOptions += "-Yno-generic-signatures",       // Suppress generation of generic signatures in bytecode
  scalacOptions += "-Xfuture",                      // Enable "future language features"
  scalacOptions in (Compile, console) --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"), // allow warnings in console

  /** Project Structure **/
  resourceDirectory in Compile := baseDirectory(_/ "resources").value,
  scalaSource in Compile := baseDirectory(_/"src").value,
  scalaSource in Test := baseDirectory(_/"test").value,

  /** Testing **/
  scalacOptions in Test ++= Seq("-Yrangepos"),

  /** Macro Paradise **/
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradise_version cross CrossVersion.full),

  /** Release **/
  publishArtifact   := true,
  publishMavenStyle := true
)


/** Projects **/
lazy val fringe = (project in file(".")).settings(settings)

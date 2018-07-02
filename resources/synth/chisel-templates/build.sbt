val scala_version = "2.11.7"

name := "templateResources"
organization := "edu.stanford.ppl"

isSnapshot := true

// scalaVersion := "2.11.7"

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:reflectiveCalls")

val defaultVersions = Map(
  "chisel3" -> "3.0-SNAPSHOT_2017-10-06",
  "chisel-iotesters" -> "1.1-SNAPSHOT"
  )

val templateResources_common = Seq(
  scalaVersion := scala_version,
  version := "1.0",

  /** External Libraries (e.g. maven dependencies) **/
  libraryDependencies ++= (Seq("chisel3","chisel-iotesters").map {
    dep: String => "edu.berkeley.cs" %% dep % sys.props.getOrElse(dep + "Version", defaultVersions(dep)) }
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
  scalacOptions += "-language:experimental.macros", // Globally enable macros
  scalacOptions += "-language:existentials",        // Globally enable existentials
  scalacOptions += "-Yno-generic-signatures",       // Suppress generation of generic signatures in bytecode
  scalacOptions += "-Xfuture",                      // Enable "future language features"
  // scalacOptions += "-opt:l:method,inline",          // Enable method optimizations, inlining
  // scalacOptions += "-opt-warnings:none",            // Disable optimization warnings

  /** Project Structure **/
  scalaSource in Compile := baseDirectory(_/"templates").value,
  scalaSource in Test := baseDirectory(_/"tests").value,

  /** Testing **/
  scalacOptions in Test ++= Seq("-Yrangepos"),
  testFrameworks += new TestFramework("utest.runner.Framework"),

  /** Macro Paradise **/
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),

  unmanagedSourceDirectories in Compile ++=  Seq(
      baseDirectory.value / "templates",
      baseDirectory.value / "fringeZynq",
      baseDirectory.value / "fringeASIC",
      baseDirectory.value / "fringeAWS",
      baseDirectory.value / "fringeVCS",
      baseDirectory.value / "fringeDE1SoC",
      baseDirectory.value / "fringeHW",
      baseDirectory.value / "emul"
    ),

  excludeFilter in unmanagedSources := "Top.scala" ,

  // Recommendations from http://www.scalatest.org/user_guide/using_scalatest_with_sbt
  logBuffered in Test := false,

  // Disable parallel execution when running te
  //  Running tests in parallel on Jenkins currently fails.
  parallelExecution in Test := false,

  /** Release **/
  publishArtifact   := true,
  publishMavenStyle := true
)



// managedSourceDirectories in Compile ++= Seq(
//    baseDirectory.value / "emul"
// )

/** Projects **/
lazy val templateResources = (project in file(".")).settings(templateResources_common)


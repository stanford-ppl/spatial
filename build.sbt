val nova_version = "1.0"
val scala_version     = "2.12.4" //"2.13.0-M3"
val paradise_version  = "2.1.0"


name := "novac"
scalaVersion := scala_version
version := nova_version

val common = Seq(
  scalaVersion := scala_version,
  version := nova_version,
  nativeLinkStubs := true,
  //crossScalaVersions := Seq("2.12.4", "2.13.0-M3"),

  /** External Libraries (e.g. maven dependencies) **/
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scala_version, // Reflection
    "com.lihaoyi" % "utest_2.12" % "0.6.3"
    //"com.lihaoyi" %% "utest" % "0.6.3" % "test"         // Testing
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
  scalacOptions += "-opt:l:method,inline",          // Enable method optimizations, inlining
  scalacOptions += "-opt-warnings:none",            // Disable optimization warnings

  /** Project Structure **/
  resourceDirectory in Compile := baseDirectory(_/ "resources").value,
  scalaSource in Compile := baseDirectory(_/"src").value,
  scalaSource in Test := baseDirectory(_/"test").value,

  /** Testing **/
  scalacOptions in Test ++= Seq("-Yrangepos"),
  testFrameworks += new TestFramework("utest.runner.Framework"),

  /** Macro Paradise **/
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradise_version cross CrossVersion.full),

  /** Release **/
  publishArtifact := false
)


/** Projects **/
lazy val emul    = project.settings(common)
lazy val forge   = project.settings(common)
lazy val core    = project.settings(common).dependsOn(forge)
lazy val nova    = project.settings(common).dependsOn(forge, emul, core)
lazy val novac   = (project in file(".")).settings(common).dependsOn(nova)

addCommandAlias("make", "compile")

name := "pcc"

val pcc_version = "1.0"
val scala_version     = "2.11.12"
val scalatest_version = "3.0.4"
val paradise_version  = "2.1.0"
enablePlugins(ScalaNativePlugin)

scalaVersion := scala_version
version := pcc_version

val commonSettings = Seq(
  scalaVersion := scala_version,
  version := pcc_version,

  /** External Libraries (e.g. maven dependencies) **/
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % scala_version,
    "org.scalatest" %% "scalatest" % scalatest_version % "test",
    "org.scala-lang.modules"  %% "scala-parser-combinators" % "1.0.4",
    "com.github.scopt" %% "scopt" % "3.5.0",
    //"com.github.pureconfig" %% "pureconfig" % "0.7.0",
    "org.apache.commons" % "commons-lang3" % "3.3.2",
    "commons-io" % "commons-io" % "2.5"
  ),

  /** Scalac Options **/
  //  scalacOptions += "-Yno-generic-signatures",       // Suppress generation of generic signatures in bytecode
  //  scalacOptions += "-opt:box-unbox",                // Optimizations
  //  scalacOptions += "-opt:copy-propagation",
  //  scalacOptions += "-opt:simplify-jumps",
  //  scalacOptions += "-opt:compact-locals",
  //  scalacOptions += "-opt:redundant-casts",
  //  scalacOptions += "-opt:nullness-tracking",
  //  scalacOptions += "-opt:closure-invocations",
  //  scalacOptions += "-opt-warnings:_",               // Optimization warnings
  //  scalacOptions += "-optimise",

  scalacOptions += "-unchecked",                    // Enable additional warnings
  scalacOptions += "-deprecation",                  // Enable warnings on deprecated usage
  scalacOptions += "-feature",                      // Warnings for features requiring explicit import
  scalacOptions += "-Xfatal-warnings",              // Warnings are errors
  scalacOptions += "-language:higherKinds",         // Globally enable higher kinded type parameters
  scalacOptions += "-language:implicitConversions", // Globally enable implicit conversions
  scalacOptions += "-language:experimental.macros", // Globally enable macros
  scalacOptions += "-language:existentials",        // Globally enable existentials

  /** Testing **/
  parallelExecution in Test := false,
  resourceDirectory in Compile := baseDirectory(_/ "resources").value,
  scalaSource in Compile := baseDirectory(_/"src").value,
  scalaSource in Test := baseDirectory(_/"test").value,

    /** Macro Paradise **/
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradise_version cross CrossVersion.full),

  /** Other **/
  publishArtifact := false
)

/** Project structure **/
lazy val forge: Project = project
  .settings(commonSettings)

lazy val core = project
  .settings(commonSettings)
  .dependsOn(forge)


addCommandAlias("make", "compile")

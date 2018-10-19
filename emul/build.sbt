val scala_version = "2.12.6"

name := "emul"
organization := "edu.stanford.cs.dawn"
isSnapshot := true

val common = Seq(
  scalaVersion := scala_version,
  version := "1.0-SNAPSHOT",
  crossScalaVersions := Seq(scala_version, "2.11.7"),

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

  /** Project Structure **/
  resourceDirectory in Compile := baseDirectory(_/ "resources").value,
  scalaSource in Compile := baseDirectory(_/"src").value,
  scalaSource in Test := baseDirectory(_/"test").value,

  /** Testing **/
  scalacOptions in Test ++= Seq("-Yrangepos"),

  /** Macro Paradise **/
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),

  /** Release **/
  publishArtifact   := true,

  isSnapshot := true,

  homepage := Some(url("https://spatial.stanford.edu")),
  scmInfo := Some(ScmInfo(url("https://github.com/stanford-ppl/spatial"),
                              "git@github.com:stanford-ppl/spatial.git")),
  developers := List(Developer("mattfel1",
                               "Matthew Feldman",
                               "mattfel@stanford.edu",
                               url("https://github.com/mattfel1")),
                     Developer("dkoeplin",
                               "David Koeplinger",
                               "dkoeplin@stanford.edu",
                               url("https://github.com/dkoeplin")),
                     Developer("raghup17",
                               "Raghu Prabhakar",
                               "raghup17@stanford.edu",
                               url("https://github.com/raghup17")),
                     Developer("yaqiz",
                               "Yaqi Zhang",
                               "yaqiz@stanford.edu",
                               url("https://github.comyaqiz/"))
                    ),
  publishTo := Some(
    if (isSnapshot.value)
      Opts.resolver.sonatypeSnapshots
    else
      Opts.resolver.sonatypeStaging
  ),

  publishMavenStyle := true

)


/** Projects **/
lazy val emul = (project in file(".")).settings(common)
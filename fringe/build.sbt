val paradise_version  = "2.1.0"
val scalatest_version = "3.0.5"
val chisel3_version   = sys.props.getOrElse("chisel3Version", "3.0-SNAPSHOT_2017-10-06")
val testers_version   = sys.props.getOrElse("chisel-iotestersVersion", "1.1-SNAPSHOT")

name := "fringe" + sys.env.get("FRINGE_PACKAGE").getOrElse("")

/** Projects **/
lazy val fringe = (project in file(".")).settings(common)

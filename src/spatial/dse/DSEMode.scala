package spatial.dse

sealed abstract class DSEMode
object DSEMode {
  case object Disabled extends DSEMode
  case object Heuristic extends DSEMode
  case object Bruteforce extends DSEMode
  case object HyperMapper extends DSEMode
  case object Experiment extends DSEMode
}

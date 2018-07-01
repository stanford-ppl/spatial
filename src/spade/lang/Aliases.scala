package spade.lang

// No aliases of the form type X = spade.lang.X (creates a circular reference)
// Everything else is ok.
trait InternalAliases extends spatial.lang.InternalAliases {


}

trait ExternalAliases extends InternalAliases with spatial.lang.ExternalAliases {
  type PCUSpec = spade.node.PCUSpec
  type PCU = spade.node.PCU
  lazy val PCU = spade.node.PCU
  type PCUModule = spade.node.PCUModule

  type PMUSpec = spade.node.PMUSpec
  type PMU = spade.node.PMU
  lazy val PMU = spade.node.PMU
  type PMUModule = spade.node.PMUModule

  type Direction = spade.node.Direction
  lazy val N  = spade.node.N
  lazy val NE = spade.node.NE
  lazy val E  = spade.node.E
  lazy val SE = spade.node.SE
  lazy val S  = spade.node.S
  lazy val SW = spade.node.SW
  lazy val W  = spade.node.W
  lazy val NW = spade.node.NW
}
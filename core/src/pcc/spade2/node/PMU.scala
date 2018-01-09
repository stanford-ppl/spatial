package pcc.spade2.node

import forge._
import pcc._
import pcc.ir._

case class PMUSpec(
  nRegs:   Int, // Number of registers per stage
  nCtrs:   Int, // Number of counters
  nLanes:  Int, // Number of vector lanes
  nStages: Int, // Number of compute stages
  nCIns:   Int, // Number of control inputs
  nCOuts:  Int, // Number of control outputs
  nSIns:   Int, // Number of scalar inputs
  nSOuts:  Int, // Number of scalar outputs
  nVIns:   Int, // Number of vector inputs
  nVOuts:  Int, // Number of vector outputs
  muxSize: Int, // TODO
  cFifoDepth: Int = 16,  // Depth of control signal FIFOs
  sFifoDepth: Int = 16,  // Depth of scalar FIFOs
  vFifoDepth: Int = 16   // Depth of vector FIFOs
)

case class PMU(eid: Int) extends Box[PMU](eid) {
  override type I = this.type
  override def fresh(id: Int): PMU = PMU(id)
  override def stagedClass: Class[PMU] = classOf[PMU]
}
object PMU {
  implicit val pmu: PMU = PMU(-1)

  @api def apply(spec: PMUSpec)(implicit wSize: Vec[Bit]): PMU = {
    implicit val vSize: Vec[Vec[Bit]] = Vec[Vec[Bit]](-1, spec.nLanes, wSize)

    val cIns  = Seq.fill(spec.nCIns){ bound[Bit] }
    val cOuts = Seq.fill(spec.nCOuts){ bound[Bit] }
    val sIns  = Seq.fill(spec.nSIns){ bound[Vec[Bit]] }
    val sOuts = Seq.fill(spec.nSOuts){ bound[Vec[Bit]] }
    val vIns  = Seq.fill(spec.nVIns){ bound[Vec[Vec[Bit]]] }
    val vOuts = Seq.fill(spec.nVOuts){ bound[Vec[Vec[Bit]]] }

    stage(PMUModule(cIns,cOuts,sIns,sOuts,vIns,vOuts,spec))
  }
}

case class PMUModule(
  cIns:  Seq[Bit],
  cOuts: Seq[Bit],
  sIns:  Seq[Vec[Bit]],
  sOuts: Seq[Vec[Bit]],
  vIns:  Seq[Vec[Vec[Bit]]],
  vOuts: Seq[Vec[Vec[Bit]]],
  spec:  PMUSpec
) extends Module[PMU] {

}
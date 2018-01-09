package pcc.spade2.node

import forge._
import pcc._
import pcc.ir._

case class PCUSpec(
  nRegs:   Int, // Number of registers per stage
  nCtrs:   Int, // Number of counters
  nLanes:  Int, // Number of vector lanes
  nStages: Int, // Number of stages
  nCIns:   Int, // Number of control inputs
  nCOuts:  Int, // Number of control outputs
  nSIns:   Int, // Number of scalar inputs
  nSOuts:  Int, // Number of scalar outputs
  nVIns:   Int, // Number of vector inputs
  nVOuts:  Int  // Number of vector outputs
)

case class PCU(eid: Int) extends Box[PCU](eid) {
  override type I = this.type

  override def fresh(id: Int): PCU = PCU(id)
  override def stagedClass: Class[PCU] = classOf[PCU]
}
object PCU {
  implicit val pcu: PCU = PCU(-1)

  @api def apply(spec: PCUSpec)(implicit wSize: Vec[Bit]): PCU = {
    implicit val vSize: Vec[Vec[Bit]] = Vec[Vec[Bit]](-1, spec.nLanes, wSize)

    val cIns  = Seq.fill(spec.nCIns){ bound[Bit] }
    val cOuts = Seq.fill(spec.nCOuts){ bound[Bit] }
    val sIns  = Seq.fill(spec.nSIns){ bound[Vec[Bit]] }
    val sOuts = Seq.fill(spec.nSOuts){ bound[Vec[Bit]] }
    val vIns  = Seq.fill(spec.nVIns){ bound[Vec[Vec[Bit]]] }
    val vOuts = Seq.fill(spec.nVOuts){ bound[Vec[Vec[Bit]]] }

    stage(PCUModule(cIns,cOuts,sIns,sOuts,vIns,vOuts,spec))
  }
}

case class PCUModule(
  cIns:   Seq[Bit],
  cOuts:  Seq[Bit],
  sIns:   Seq[Vec[Bit]],
  sOuts:  Seq[Vec[Bit]],
  vIns:   Seq[Vec[Vec[Bit]]],
  vOuts:  Seq[Vec[Vec[Bit]]],
  spec:   PCUSpec
) extends Module[PCU] {

}

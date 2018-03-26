package spade.node

import argon._
import forge.tags._

import spatial.lang._
import pir.lang._

class PMUSpec(
  val nRegs:   Int, // Number of registers per stage
  val nCtrs:   Int, // Number of counters
  val nLanes:  Int, // Number of vector lanes
  val nStages: Int, // Number of compute stages
  val muxSize: Int, // TODO
  val cFifoDepth: Int = 16,  // Depth of control signal FIFOs
  val sFifoDepth: Int = 16,  // Depth of scalar FIFOs
  val vFifoDepth: Int = 16,  // Depth of vector FIFOs
  val cIns    : List[Direction], // Control input directions
  val cOuts   : List[Direction], // Control output directions
  val sIns    : List[Direction], // Scalar input directions
  val sOuts   : List[Direction], // Scalar output directions
  val vIns    : List[Direction], // Vector input directions
  val vOuts   : List[Direction]  // Vector output directions
) extends PUSpec

@ref class PMU extends Box[PMU]

object PMU {
  @api def apply(spec: PMUSpec)(implicit wSize: Vec[Bit]): PMU = {
    implicit val vSize: Vec[Vec[Bit]] = Vec.bits[Vec[Bit]](spec.nLanes)

    val cIns  = Seq.fill(spec.nCIns){ bound[In[Bit]] }
    val cOuts = Seq.fill(spec.nCOuts){ bound[Out[Bit]] }
    val sIns  = Seq.fill(spec.nSIns){ bound[In[Vec[Bit]]] }
    val sOuts = Seq.fill(spec.nSOuts){ bound[Out[Vec[Bit]]] }
    val vIns  = Seq.fill(spec.nVIns){ bound[In[Vec[Vec[Bit]]]] }
    val vOuts = Seq.fill(spec.nVOuts){ bound[Out[Vec[Vec[Bit]]]] }

    stage(PMUModule(cIns,cOuts,sIns,sOuts,vIns,vOuts,spec))
  }
}

@op case class PMUModule(
  cIns:   Seq[In[Bit]],
  cOuts:  Seq[Out[Bit]],
  sIns:   Seq[In[Vec[Bit]]],
  sOuts:  Seq[Out[Vec[Bit]]],
  vIns:   Seq[In[Vec[Vec[Bit]]]],
  vOuts:  Seq[Out[Vec[Vec[Bit]]]],
  spec:  PMUSpec
) extends PUModule[PMU]

package pcc.test

import pcc.lang._
//import pcc.lang.pir.{In, Out}
import pcc.spade.node._
import utest._

object ArchModel_Simple extends Test {
  override val isPIR = true
  override val isArchModel = true

  val pcuSpec = new PCUSpec(
    nRegs   = 6,
    nCtrs   = 5,
    nLanes  = 16,
    nStages = 6,
    cIns   = List(NE, NE, SE, SE, SW, SW, NW, NW),
    cOuts  = List(NE, NE, SE, SE, SW, SW, NW, NW),
    sIns   = List(NE, SE, SW, NW),
    sOuts  = List(NE, SE, SW, NW),
    vIns   = List(NE, SE, SW, NW),
    vOuts  = List(NE, SE, SW, NW)
    )

  val pmuSpec = new PMUSpec(
    nRegs   = 6,
    nCtrs   = 5,
    nLanes  = 16,
    nStages = 6,
    muxSize  = 2,
    cFifoDepth = 16,
    sFifoDepth = 16,
    vFifoDepth = 8,
    cIns   = List(NE, NE, SE, SE, SW, SW, NW, NW),
    cOuts  = List(NE, NE, SE, SE, SW, SW, NW, NW),
    sIns   = List(NE, SE, SW, NW),
    sOuts  = List(NE, SE, SW, NW),
    vIns   = List(NE, SE, SW, NW),
    vOuts  = List(NE, SE, SW, NW)
    )

  val SRAM_SIZE_BYTES = 256 * 1024
  val SRAM_SIZE_WORDS = SRAM_SIZE_BYTES / 4

  def main(): Void = {
    implicit val wSize: Vec[Bit] = Vec.tp(pmuSpec.nLanes)  // word width
    def getPMU(x: Int, y: Int) = {
      val m = PMU(pmuSpec).op.get.asInstanceOf[PMUModule]
      m.x = x
      m.y = y
      m
    }
    def getPCU(x: Int, y: Int) = {
      val m = PCU(pcuSpec).op.get.asInstanceOf[PCUModule]
      m.x = x
      m.y = y
      m
    }

    def grid(rows: Int, cols: Int) = List.tabulate(rows) { y =>
      List.tabulate(cols) { x =>
        val flattenedID = x * cols + y
        if (flattenedID % 2 == 0) getPCU(x, y) else getPMU(x, y)
      }
    }

    val rows = 4
    val cols = 4
    val g = grid(rows, cols)

    g(0)(1).vOut(NW, 0).get ==> g(0)(0).vIn(NE, 0).get
    g(0)(0).vOut(NE, 0).get ==> g(0)(1).vIn(NW, 0).get

//    y.vOuts(0) ==> x.vIns(0)
  }
}

object ArchModelTests extends Testbench { val tests = Tests {
  'ArchModel_Simple - test(ArchModel_Simple)
}}

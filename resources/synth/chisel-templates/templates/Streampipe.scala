// See LICENSE.txt for license details.
package templates

import chisel3._


class Streampipe(override val n: Int, override val ctrDepth: Int = 1, override val isFSM: Boolean = false, override val stateWidth: Int = 32, override val retime: Int = 0, override val staticNiter: Boolean = false, override val isReduce: Boolean = false) extends Parallel(n) {
}


// Inner pipe
class Streaminner(val isFSM: Boolean = false, val ctrDepth: Int = 1, val stateWidth: Int = 32, val retime: Int = 0, val staticNiter: Boolean = false, val isReduce: Boolean = false) extends Module {

  // States
  val pipeInit = 0
  val pipeRun = 1
  val pipeDone = 2
  val pipeSpinWait = 3

  // Module IO
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val numIter = Input(UInt(32.W))
      val ctr_done = Input(Bool())
      val forever = Input(Bool())
      val rst = Input(Bool())
      val hasStreamIns = Input(Bool()) // If there is a streamIn for this stage, then we should not require en=true for done to go high

      // FSM signals
      val nextState = Input(UInt(stateWidth.W))
      val initState = Input(UInt(stateWidth.W))
      val doneCondition = Input(Bool())

    }
    val output = new Bundle {
      val done = Output(Bool())
      val ctr_inc = Output(Bool())
      val rst_en = Output(Bool())
      // FSM signals
      val state = Output(UInt(stateWidth.W))
    }
  })

  if (!isFSM) {
    val state = RegInit(pipeInit.U)
    io.output.done := Mux(io.input.forever, false.B, Mux(io.input.ctr_done & Mux(io.input.hasStreamIns, true.B, io.input.enable), true.B, false.B)) // If there is a streamIn for this stage, then we should not require en=true for done to go high
    io.output.rst_en := false.B
  } else { // FSM inner
    val stateFSM = Module(new FF(stateWidth))
    val doneReg = Module(new SRFF())

    stateFSM.io.input(0).data := io.input.nextState
    stateFSM.io.input(0).init := io.input.initState
    stateFSM.io.input(0).enable := io.input.enable
    stateFSM.io.input(0).reset := reset
    io.output.state := stateFSM.io.output.data

    doneReg.io.input.set := io.input.doneCondition & io.input.enable
    doneReg.io.input.reset := ~io.input.enable
    doneReg.io.input.asyn_reset := false.B
    val streamMask = Mux(io.input.hasStreamIns, true.B, io.input.enable)
    io.output.done := streamMask & (doneReg.io.output.data | (io.input.doneCondition & io.input.enable))

  }

}

// See LICENSE.txt for license details.
package templates

import chisel3._

//A n-stage Parallel controller
class Parallel(val n: Int, val ctrDepth: Int = 1, val isFSM: Boolean = false, val stateWidth: Int = 32, val retime:Int = 0, val staticNiter: Boolean = false, val isReduce: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val numIter = Input(UInt(32.W))
      val stageDone = Vec(n, Input(Bool()))
      val stageMask = Vec(n, Input(Bool()))
      val forever = Input(Bool())
      val rst = Input(Bool())
      val hasStreamIns = Input(Bool()) // Not used, here for codegen compatibility

      // FSM signals
      val nextState = Input(SInt(stateWidth.W))
    }
    val output = new Bundle {
      val done = Output(Bool())
      val stageEnable = Vec(n, Output(Bool()))
      val rst_en = Output(Bool())
      val ctr_inc = Output(Bool())
      // FSM signals
      val state = Output(SInt(stateWidth.W))
    }
  })

  // 0: INIT, 1: Separate reset from enables, 2 stages enabled, 3: DONE
  // Name the universal states
  val initState = 0
  val bufferState = initState + 1
  val runningState = bufferState + 1
  val doneState = runningState + 1

  // Create FF for holding state
  val stateFF = Module(new FF(2))
  stateFF.io.input(0).enable := true.B
  stateFF.io.input(0).init := bufferState.U
  stateFF.io.input(0).reset := io.input.rst
  val state = stateFF.io.output.data

  // Create vector of registers for holding stage dones
  val doneFF = List.tabulate(n) { i =>
    val ff = Module(new SRFF())
    ff.io.input.set := (io.input.stageDone(i) | ~io.input.stageMask(i)) & io.input.enable
    // Not 100% sure that this reset delay is correct.  Originally included because a mask on one lane of a parallelized
    //   controller was holding this doneFF lane done even after the particular masked iteration was done
    ff.io.input.asyn_reset := state === doneState.U | state === bufferState.U | Utils.getRetimed(state === doneState.U,retime)
    ff.io.input.reset := (state === doneState.U) | state === bufferState.U | io.input.rst | Utils.getRetimed(state === doneState.U,retime)
    ff
  }
  val doneMask = doneFF.map { _.io.output.data }

  // // Provide default value for enable and doneClear
  // io.output.stageEnable.foreach { _ := Bool(false) }

  io.output.rst_en := false.B

  io.output.stageEnable.foreach { s => s := false.B}
  // State Machine
  when(io.input.enable) {
    // when(state === initState.U) {   // INIT -> RESET
    //   stateFF.io.input(0).data := bufferState.U
    //   io.output.rst_en := true.B
    // }
    when (state === bufferState.U) { // Not sure if this state is needed for stream
      io.output.rst_en := true.B
      stateFF.io.input(0).data := runningState.U
    }.elsewhen (state === runningState.U) {  // STEADY
      (0 until n).foreach { i => io.output.stageEnable(i) := Mux(io.input.forever, true.B, Mux(io.input.stageMask(i), ~doneMask(i), false.B)) }

      val doneTree = doneMask.reduce { _ & _ }
      when(doneTree === 1.U) {
        stateFF.io.input(0).data := Mux(io.input.forever, runningState.U, doneState.U)
      }.otherwise {
        stateFF.io.input(0).data := state
      }
    }.elsewhen (state === doneState.U) {  // DONE
      stateFF.io.input(0).data := bufferState.U
    }.otherwise {
      stateFF.io.input(0).data := state
    }
  }.otherwise {
    stateFF.io.input(0).data := bufferState.U
    (0 until n).foreach { i => io.output.stageEnable(i) := false.B }
  }

  // Output logic
  io.output.done := Mux(io.input.forever, false.B, state === runningState.U && doneMask.reduce{_&_})
  io.output.ctr_inc := false.B // No counters for parallels (BUT MAYBE NEEDED FOR STREAMPIPES)
}






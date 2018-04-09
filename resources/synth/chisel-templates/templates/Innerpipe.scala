package templates

import chisel3._

import scala.collection.mutable.HashMap

// Inner pipe
class Innerpipe(val isFSM: Boolean = false, val ctrDepth: Int = 1, val stateWidth: Int = 32, val retime: Int = 0, val staticNiter: Boolean = false, val isReduce: Boolean = false) extends Module {

  // States
  val pipeInit = 0
  val pipeReset = 1
  val pipeRun = 2
  val pipeDone = 3
  val pipeSpinWait = 4

  // Module IO
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val numIter = Input(UInt(32.W))
      val ctr_done = Input(Bool())
      val rst = Input(Bool())
      val forever = Input(Bool())
      val hasStreamIns = Input(Bool()) // Not used, here for codegen compatibility

      // FSM signals
      val nextState = Input(SInt(stateWidth.W))
      val initState = Input(SInt(stateWidth.W))
      val doneCondition = Input(Bool())
    }
    val output = new Bundle {
      val done = Output(Bool())
      val extendedDone = Output(Bool()) // For the Innerpipe chisel template test, since we need done to be on for the next falling edge
      val ctr_inc = Output(Bool())
      val rst_en = Output(Bool())
      // FSM signals
      val state = Output(SInt(stateWidth.W))
    }
  })

  if (!isFSM) {

    val rstLatch = Module(new SRFF())
    rstLatch.io.input.set := io.input.rst
    rstLatch.io.input.reset := io.input.enable
    rstLatch.io.input.asyn_reset := io.input.enable

    // Had to change state to a spatial FF because chisel had some bullshit bug where it wrote the completely wrong number to the RegInit as of Sept 26, 2017
    val stateFF = Module(new FF(32))
    stateFF.io.input(0).enable := true.B
    stateFF.io.input(0).reset := false.B//io.input.rst
    stateFF.io.input(0).init := {if (isReduce) pipeReset.U else pipeRun.U}
    val state = stateFF.io.output.data
    //val state = RegInit(pipeReset.U(32.W))

    // // Initialize state and maxFF
    // val rstCtr = Module(new SingleCounter(1,Some(0), Some(1), Some(1), Some(0)))
    // rstCtr.io.input.enable := state === pipeReset.U & io.input.enable
    // rstCtr.io.input.reset := (state != pipeReset.U && state < pipeSpinWait.U) | io.input.rst
    // rstCtr.io.input.saturate := true.B
    // rstCtr.io.input.stop := 1.S
    // rstCtr.io.input.gap := 0.S
    // rstCtr.io.input.start := 0.S
    // rstCtr.io.input.stride := 1.S
    val rst = ~io.input.enable | io.input.rst | (state =/= pipeDone.U & state =/= pipeReset.U)

    io.output.rst_en := Utils.getRetimed(( (state =/= pipeRun.U & state =/= pipeReset.U /*sic*/) || rstLatch.io.output.data), 1) || io.input.rst

    // Only start the state machine when the enable signal is set
    when (io.input.enable) {
      // Change states
      // when( state === pipeInit.U ) {
      //   io.output.done := false.B
      //   io.output.ctr_inc := false.B
      //   io.output.rst_en := false.B
      //   state := pipeReset.U
      when( state === pipeReset.U ) {
        io.output.done := false.B
        io.output.extendedDone := false.B
        io.output.ctr_inc := false.B
        // io.output.rst_en := true.B;
        stateFF.io.input(0).data := Mux(io.input.ctr_done, pipeDone.U, pipeRun.U) // Shortcut to done state, for tile store
        when (rst) {
          // io.output.rst_en := false.B
          stateFF.io.input(0).data := Mux(io.input.ctr_done, pipeDone.U, pipeReset.U) // Shortcut to done state, for tile store
        }
      }.elsewhen( state === pipeRun.U ) {
        // io.output.rst_en := false.B
        io.output.ctr_inc := Mux(io.input.enable && ~(Utils.getRetimed(state =/= pipeRun.U || rstLatch.io.output.data,1) || io.input.rst), true.B, false.B)
        when (io.input.ctr_done) {
          io.output.done := Mux(io.input.forever, false.B, true.B)
          io.output.extendedDone := Mux(io.input.forever, false.B, true.B)
          io.output.ctr_inc := false.B
          stateFF.io.input(0).data := pipeDone.U
        }.otherwise {
          io.output.done := false.B
          io.output.extendedDone := false.B
          stateFF.io.input(0).data := pipeRun.U
        }
      }.elsewhen( state === pipeDone.U ) {
        // io.output.rst_en := false.B
        io.output.ctr_inc := false.B
        io.output.done := false.B//Mux(io.input.forever, false.B, true.B)
        io.output.extendedDone := Mux(io.input.forever, false.B, true.B)
        if (retime == 0) stateFF.io.input(0).data := {if (isReduce) pipeReset.U else pipeRun.U} else stateFF.io.input(0).data := pipeSpinWait.U
      }.elsewhen( state >= pipeSpinWait.U ) {
        io.output.ctr_inc := false.B
        // io.output.rst_en := false.B
        io.output.done := false.B
        io.output.extendedDone := false.B
        stateFF.io.input(0).data := Mux(state >= (pipeSpinWait + retime).U, {if (isReduce) pipeReset.U else pipeRun.U}, state + 1.U);
      } 
    }.otherwise {
      io.output.done := Mux(io.input.ctr_done | (state === pipeRun.U & io.input.ctr_done), true.B, false.B)
      io.output.ctr_inc := false.B
      // io.output.rst_en := false.B
      io.output.state := state.asSInt
      if (retime == 0) {
        stateFF.io.input(0).data := {if (isReduce) pipeReset.U else pipeRun.U}
      } else {
        stateFF.io.input(0).data := Mux(state === pipeDone.U, pipeSpinWait.U, Mux(state === pipeRun.U & io.input.ctr_done, pipeDone.U, state)) // Move along if enable turns on just as we reach done state
      }
    }
  } else { // FSM inner
    val stateFSM = Module(new FF(stateWidth))
    val doneReg = Module(new SRFF())

    stateFSM.io.input(0).data := io.input.nextState.asUInt
    stateFSM.io.input(0).init := io.input.initState.asUInt
    stateFSM.io.input(0).enable := io.input.enable
    stateFSM.io.input(0).reset := reset.toBool | ~io.input.enable
    io.output.state := stateFSM.io.output.data.asSInt

    doneReg.io.input.set := io.input.doneCondition & io.input.enable
    doneReg.io.input.reset := ~io.input.enable
    doneReg.io.input.asyn_reset := false.B
    io.output.done := doneReg.io.output.data | (io.input.doneCondition & io.input.enable)

  }
}

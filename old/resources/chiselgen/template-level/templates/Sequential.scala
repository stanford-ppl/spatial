// See LICENSE.txt for license details.
package templates

import ops._
import chisel3._
import Utils._
import scala.collection.mutable.HashMap

class Seqpipe(val n: Int, val ctrDepth: Int = 1, val isFSM: Boolean = false, val stateWidth: Int = 32, val retime: Int = 0, val staticNiter: Boolean = false, val isReduce: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val numIter = Input(UInt(32.W))
      val stageDone = Vec(n, Input(Bool()))
      val stageMask = Vec(n, Input(Bool()))
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
      val stageEnable = Vec(n, Output(Bool()))
      val rst_en = Output(Bool())
      val ctr_inc = Output(Bool())
      // FSM signals
      val state = Output(SInt(stateWidth.W))
    }
  })

  if (!isFSM) {
    // 0: INIT, 1: RESET, 2..2+n-1: stages, n: DONE
    val initState = 0
    val resetState = 1
    val firstState = resetState + 1
    val doneState = firstState + n
    val lastState = doneState - 1

    val niterComputeDelay = (ctrDepth * (fixmul_latency*32).toInt + Utils.delay_per_numIter + 1).toInt
    val rstMax = if (staticNiter) 1 else niterComputeDelay
    val rstw = Utils.log2Up(niterComputeDelay) + 2
    val rstCtr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0), width = rstw))
    val firstIterComplete = Module(new SRFF())
    firstIterComplete.io.input.set := rstCtr.io.output.done
    firstIterComplete.io.input.reset := Utils.getRetimed(reset, 1)
    firstIterComplete.io.input.asyn_reset := Utils.getRetimed(reset, 1)

    val stateFF = Module(new FF(32))
    stateFF.io.input(0).enable := true.B // TODO: Do we need this line?
    stateFF.io.input(0).init := resetState.U
    stateFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1) // Delay to break critical path @ 250MHz
    val state = stateFF.io.output.data.asSInt

    rstCtr.io.input.enable := state === resetState.S & io.input.enable
    rstCtr.io.input.reset := (state != resetState.S) | Utils.getRetimed(io.input.rst, 1)
    rstCtr.io.input.saturate := true.B
    rstCtr.io.input.stop := Mux(firstIterComplete.io.output.data, rstMax.S(rstw.W), niterComputeDelay.S(rstw.W))
    rstCtr.io.input.gap := 0.S(rstw.W)
    rstCtr.io.input.start := 0.S(rstw.W)
    rstCtr.io.input.stride := 1.S(rstw.W)

    // Counter for num iterations
    val maxFF = Module(new FF(32))
    maxFF.io.input(0).enable := io.input.enable
    maxFF.io.input(0).data := io.input.numIter
    maxFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1)
    maxFF.io.input(0).init := 0.U
    val max = getRetimed(maxFF.io.output.data,1)

    val ctr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0)))
    ctr.io.input.enable := io.input.enable & io.input.stageDone(lastState-2) // TODO: Is this wrong? It still works...  
    ctr.io.input.saturate := false.B
    ctr.io.input.stop := max.asSInt
    ctr.io.input.reset := Utils.getRetimed(io.input.rst, 1) | (state === doneState.S)
    val iter = ctr.io.output.count(0)
    io.output.rst_en := getRetimed((state === resetState.S),1)

    when(io.input.enable) {
      // when(state === initState.S) {
      //   stateFF.io.input(0).data := resetState.U
      //   io.output.stageEnable.foreach { s => s := false.B}
      // }
      when (state === resetState.S) {
        stateFF.io.input(0).data := Mux(io.input.numIter === 0.U, 
                    Mux(io.input.forever, firstState.U, Mux(rstCtr.io.output.done, doneState.U, resetState.U)), 
                    Mux(rstCtr.io.output.done, firstState.U, resetState.U))
        io.output.stageEnable.foreach { s => s := false.B}
      }.elsewhen (state < lastState.S) {

        // // Safe but expensive way
        // val doneStageId = (0 until n).map { i => // Find which stage got done signal
        //   Mux(io.input.stageDone(i), UInt(i+1), 0.U) 
        // }.reduce {_+_}
        // when(state === (doneStageId + 1.U)) {
        //   stateFF.io.input(0).data := doneStageId + 2.U
        // }.otherwise {
        //   stateFF.io.input(0).data := state
        // }

        // // Less safe but cheap way
        // val aStageIsDone = io.input.stageDone.reduce { _ | _ } // TODO: Is it safe to assume children behave properly?
        // when(aStageIsDone) {
        //   stateFF.io.input(0).data := state + 1.U
        // }.otherwise {
        //   stateFF.io.input(0).data := state
        // }
        // Correct way
        val stageDones = (0 until n).map{i => (i.U -> {io.input.stageDone(i) | ~io.input.stageMask(i)} )}
        val myStageIsDone = chisel3.util.MuxLookup( (state - firstState.S).asUInt, false.B, stageDones) 
        when(myStageIsDone) {
          stateFF.io.input(0).data := (state + 1.S).asUInt
        }.otherwise {
          stateFF.io.input(0).data := state.asUInt
        }

      }.elsewhen (state === lastState.S) {
        when(io.input.stageDone(lastState-2)) {
          when(ctr.io.output.done) {
            stateFF.io.input(0).data := Mux(io.input.forever, firstState.U, doneState.U)
          }.otherwise {
            stateFF.io.input(0).data := firstState.U
          }
        }.otherwise {
          stateFF.io.input(0).data := state.asUInt
        }

      }.elsewhen (state === doneState.S) {
        stateFF.io.input(0).data := resetState.U
      }.otherwise {
        stateFF.io.input(0).data := state.asUInt
      }
    }.otherwise {
      stateFF.io.input(0).data := resetState.U
    }
  //  stateFF.io.input(0).data := nextStateMux.io.out

    // Output logic
    io.output.done := state === doneState.S
    io.output.ctr_inc := io.input.stageDone(n-1) & Utils.delay(~io.input.stageDone(0), 1) // on rising edge
    io.output.stageEnable.zipWithIndex.foreach { case (en, i) => en := (state === (i+2).S) }
    io.output.state := state
  } else { // FSM logic
    // 0: INIT, 1: RESET, 2..2+n-1: stages, n: DONE
    val initState = 0
    val resetState = 1
    val firstState = resetState + 1
    val doneState = firstState + n
    val retimeWaitState = doneState + 1
    val lastState = doneState - 1

    val stateFF = Module(new FF(32))
    stateFF.io.input(0).enable := true.B // TODO: Do we need this line?
    stateFF.io.input(0).init := 0.U
    stateFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1)
    val state = stateFF.io.output.data.asSInt

    // FSM stuff 
    val stateFSM = Module(new FF(stateWidth))
    val doneReg = Module(new SRFF())

    stateFSM.io.input(0).data := io.input.nextState.asUInt
    stateFSM.io.input(0).init := io.input.initState.asUInt
    stateFSM.io.input(0).reset := reset.toBool | Utils.getRetimed(io.input.rst, 1)
    // Delay below is potentially dangerous if we have a delay so long that this runs into the next FSM body
    stateFSM.io.input(0).enable := getRetimed(io.input.enable & state === doneState.S, retime)
    io.output.state := stateFSM.io.output.data.asSInt

    doneReg.io.input.set := io.input.doneCondition & io.input.enable
    doneReg.io.input.reset := ~io.input.enable
    doneReg.io.input.asyn_reset := false.B
    io.output.done := doneReg.io.output.data | (io.input.doneCondition & io.input.enable)

    // Counter for num iterations
    val maxFF = Module(new FF(32))
    maxFF.io.input(0).enable := io.input.enable
    maxFF.io.input(0).data := io.input.numIter
    maxFF.io.input(0).init := 0.U
    maxFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1)
    val max = maxFF.io.output.data.asSInt

    val ctr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0)))
    ctr.io.input.enable := io.input.enable & io.input.stageDone(lastState-2) // TODO: Is this wrong? It still works...  
    ctr.io.input.reset := (state === doneState.S)
    ctr.io.input.saturate := false.B
    ctr.io.input.stop := max.asSInt
    val iter = ctr.io.output.count(0)
    io.output.rst_en := (state === resetState.S)

    when(io.input.enable) {
      when(state === initState.S) {
        stateFF.io.input(0).data := resetState.U
        io.output.stageEnable.foreach { s => s := false.B}
      }.elsewhen (state === resetState.S) {
        stateFF.io.input(0).data := Mux(io.input.numIter === 0.U, Mux(io.input.forever, firstState.U, doneState.U), firstState.U)
        io.output.stageEnable.foreach { s => s := false.B}
      }.elsewhen (state < lastState.S) {
        val stageDones = (0 until n).map{i => (i.U -> {io.input.stageDone(i) | ~io.input.stageMask(i)} )}
        val myStageIsDone = chisel3.util.MuxLookup( (state - firstState.S).asUInt, false.B, stageDones) 
        when(myStageIsDone) {
          stateFF.io.input(0).data := (state + 1.S).asUInt
        }.otherwise {
          stateFF.io.input(0).data := state.asUInt
        }

        when(myStageIsDone) {
          stateFF.io.input(0).data := (state + 1.S).asUInt
        }.otherwise {
          stateFF.io.input(0).data := state.asUInt
        }

      }.elsewhen (state === lastState.S) {
        when(io.input.stageDone(lastState-2)) {
          when(ctr.io.output.done) {
            stateFF.io.input(0).data := Mux(io.input.forever, firstState.U, doneState.U)
          }.otherwise {
            stateFF.io.input(0).data := firstState.U
          }
        }.otherwise {
          stateFF.io.input(0).data := state.asUInt
        }

      }.elsewhen (state === doneState.S) {
        val afterDone = if (retime > 0) retimeWaitState else initState
        stateFF.io.input(0).data := afterDone.U
      }.elsewhen (state >= retimeWaitState.S) {
        stateFF.io.input(0).data := Mux(state - retimeWaitState.S < retime.S, (state + 1.S).asUInt, initState.U)
      }.otherwise {
        stateFF.io.input(0).data := state.asUInt
      }
    }.otherwise {
      stateFF.io.input(0).data := initState.U
    }
  //  stateFF.io.input(0).data := nextStateMux.io.out

    // Output logic
    io.output.ctr_inc := io.input.stageDone(n-1) & Utils.delay(~io.input.stageDone(0), 1) // on rising edge
    io.output.stageEnable.zipWithIndex.foreach { case (en, i) => en := (state === (i+2).S) }
  }
}

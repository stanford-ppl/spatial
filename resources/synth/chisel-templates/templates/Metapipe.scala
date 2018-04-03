// See LICENSE.txt for license details.
package templates

import ops._
import chisel3._
import chisel3.util._
import Utils._

import scala.collection.mutable.HashMap

class Metapipe(val n: Int, val ctrDepth: Int = 1, val isFSM: Boolean = false, val stateWidth: Int = 32, val numIterWidth: Int = 32, val retime: Int = 0, val staticNiter: Boolean = false, isReduce: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val input = new Bundle {
      val enable = Input(Bool())
      val numIter = Input(UInt(numIterWidth.W))
      val stageDone = Vec(n, Input(Bool()))
      val stageMask = Vec(n, Input(Bool()))
      val rst = Input(Bool())
      val forever = Input(Bool())
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

  def bitsToAddress(k:Int) = {(scala.math.log(k)/scala.math.log(2)).toInt + 1}


  // // Counter for num iterations
  // val maxFF = Module(new FF(numIterWidth))
  // maxFF.io.input(0).enable := io.input.enable
  // maxFF.io.input(0).data := io.input.numIter
  // maxFF.io.input(0).init := 0.U
  // maxFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1)
  // val max = getRetimed(maxFF.io.output.data,1)
  // val active = List.tabulate(n){i => Module(new SRFF())}
  // val done = List.tabulate(n){i => Module(new SRFF())}
  // val firstStageDone = active(0).io.output.data === 1.U && Utils.risingEdge(io.input.stageDone(0))
  // val allDone = done.map(_.io.output.data).reduce{_&&_}
  // val last = Module(new FF(n+1))
  // val lastIterDone = last.io.output.data(n)
  // val niterComputeDelay = (ctrDepth * (fixmul_latency*32).toInt + Utils.delay_per_numIter + 1).toInt
  // val rstMax = if (staticNiter) 1 else niterComputeDelay
  // val rstw = Utils.log2Up(niterComputeDelay) + 2
  // val rstCtr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0), width = rstw))
  // val running = Module(new SRFF())

  // val entryCtr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0)))
  // entryCtr.io.input.enable := firstStageDone
  // entryCtr.io.input.saturate := true.B
  // entryCtr.io.input.stop := max.asSInt
  // entryCtr.io.input.reset := Utils.getRetimed(io.input.rst | lastIterDone, 1)

  // last.io.input(0).data := Mux(maxFF.io.output.data === 0.U && rstCtr.io.output.done, 
  //                              chisel3.util.Cat(true.B, Fill(n, false.B)), 
  //                              Mux(entryCtr.io.output.done, 1.U, last.io.output.data << 1)
  //                             )
  // last.io.input(0).enable := allDone

  // for(i <- 0 until n) {
  //   active(i).io.input.reset := allDone
  //   if (i == 0) {active(i).io.input.set := io.input.enable & rstCtr.io.output.done & ~entryCtr.io.output.done} 
  //   else {active(i).io.input.set := active(i-1).io.output.data && allDone}

  //   done(i).io.input.set := io.input.stageDone(i)
  //   done(i).io.input.reset := allDone
  // }

  // val firstIterComplete = Module(new SRFF())
  // firstIterComplete.io.input.set := rstCtr.io.output.done
  // firstIterComplete.io.input.reset := Utils.getRetimed(reset, 1)
  // firstIterComplete.io.input.asyn_reset := Utils.getRetimed(reset, 1)
  // rstCtr.io.input.enable := io.input.enable && ~running.io.output.data 
  // rstCtr.io.input.reset := Utils.getRetimed(io.input.rst, 1) && running.io.output.data
  // rstCtr.io.input.saturate := true.B
  // rstCtr.io.input.stop := Mux(staticNiter.B, 1.S, Mux(firstIterComplete.io.output.data, rstMax.S(rstw.W), niterComputeDelay.S(rstw.W)))
  // rstCtr.io.input.gap := 0.S(rstw.W)
  // rstCtr.io.input.start := 0.S(rstw.W)
  // rstCtr.io.input.stride := 1.S(rstw.W)

  // running.io.input.set := rstCtr.io.output.done
  // running.io.input.reset := io.input.rst

  // io.output.stageEnable.zip(active).foreach{case (en, t) => en := t.io.output.data}
  // io.output.ctr_inc := firstStageDone
  // io.output.done := lastIterDone
  // io.output.rst_en := ~running.io.output.data



  // 0: INIT, 1: RESET, 2..2+n-1: stages, n: DONE
  val initState = 0
  val resetState = 1
  val fillState = resetState + 1
  val steadyState = fillState + n - 1
  val drainState = steadyState + 1
  val doneState = drainState+n-1

  val deadState = Module(new SRFF()) // This is a hack because with new retime optimizations, mask signal may come one cycle after next state is entered
  deadState.io.input.asyn_reset := Utils.getRetimed(reset, 1)

  val niterComputeDelay = (ctrDepth * (fixmul_latency*32).toInt + Utils.delay_per_numIter + 1).toInt
  val rstMax = if (staticNiter) 1 else niterComputeDelay
  val rstw = Utils.log2Up(niterComputeDelay) + 2
  val rstCtr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0), width = rstw))
  val firstIterComplete = Module(new SRFF())
  firstIterComplete.io.input.set := rstCtr.io.output.done
  firstIterComplete.io.input.reset := Utils.getRetimed(reset, 1)
  firstIterComplete.io.input.asyn_reset := Utils.getRetimed(reset, 1)

  val stateFF = Module(new FF(numIterWidth))
  stateFF.io.input(0).enable := true.B // TODO: Do we need this line?
  stateFF.io.input(0).init := resetState.U
  stateFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1) // Delay to break critical path @ 250MHz
  val state = stateFF.io.output.data

  rstCtr.io.input.enable := state === resetState.U & io.input.enable
  rstCtr.io.input.reset := (state != resetState.U) | Utils.getRetimed(io.input.rst, 1)
  rstCtr.io.input.saturate := true.B
  // rstCtr.io.input.stop := Mux(firstIterComplete.io.output.data, rstMax.S(rstw.W), niterComputeDelay.S(rstw.W))
  rstCtr.io.input.stop := Mux(staticNiter.B, 1.S, Mux(firstIterComplete.io.output.data, rstMax.S(rstw.W), niterComputeDelay.S(rstw.W)))
  rstCtr.io.input.gap := 0.S(rstw.W)
  rstCtr.io.input.start := 0.S(rstw.W)
  rstCtr.io.input.stride := 1.S(rstw.W)

  // Counter for num iterations
  val maxFF = Module(new FF(numIterWidth))
  maxFF.io.input(0).enable := io.input.enable
  maxFF.io.input(0).data := io.input.numIter
  maxFF.io.input(0).init := 0.U
  maxFF.io.input(0).reset := Utils.getRetimed(io.input.rst, 1)
  val max = getRetimed(maxFF.io.output.data,1)

  val doneClear = Wire(UInt())
  val doneFF = List.tabulate(n) { i =>
    val ff = Module(new SRFF())
    ff.io.input.set := io.input.stageDone(i) //& ~deadState.io.output.data
    ff.io.input.asyn_reset := Utils.getRetimed(io.input.rst, 1)
    ff.io.input.reset := doneClear //false.B
    ff
  }
  val doneMask = doneFF.zipWithIndex.map { case (ff, i) => ff.io.output.data }

  val ctr = Module(new SingleCounter(1, Some(0), None, Some(1), Some(0)))
  ctr.io.input.enable := doneClear //& ~deadState.io.output.data
  ctr.io.input.saturate := true.B
  ctr.io.input.stop := max.asSInt
  ctr.io.input.reset := Utils.getRetimed(io.input.rst | (state === doneState.U), 1)
  io.output.rst_en := getRetimed((state === resetState.U),1)

  // Counter for handling drainage while in fill state
  val cycsSinceDone = Module(new FF(bitsToAddress(n)))
  cycsSinceDone.io.input(0).init := 0.U
  cycsSinceDone.io.input(0).reset := (state === doneState.U)

  // // Provide default value for enable and doneClear
  // io.output.stageEnable.foreach { _ := UInt(0) }
  // doneClear := UInt(0)

  when(io.input.enable ) {
    deadState.io.input.reset := false.B
    // when(state === initState.U) {   // INIT -> RESET
    //   stateFF.io.input(0).data := resetState.U
    //   doneClear := false.B
    //   io.output.stageEnable.foreach { s => s := false.B}
    //   cycsSinceDone.io.input(0).enable := false.B
    // }
    when (state === resetState.U) {  // RESET -> FILL
      doneClear := false.B
      stateFF.io.input(0).data := Mux(io.input.numIter === 0.U(numIterWidth.W), 
                          Mux(io.input.forever, steadyState.U, Mux(rstCtr.io.output.done, doneState.U, resetState.U)), 
                          Mux(rstCtr.io.output.done, fillState.U, resetState.U)) // Go directly to done if niters = 0
      io.output.stageEnable.foreach { s => s := false.B}
      cycsSinceDone.io.input(0).enable := false.B
    }.elsewhen (state < steadyState.U) {  // FILL -> STEADY
      for ( i <- fillState until steadyState) {
        val fillStateID = i - fillState
        when((state === i.U)) {
          io.output.stageEnable.zip(doneMask).zipWithIndex.take(fillStateID+1).foreach { 
            case ((en, done), ii) => 
              en := ~done & (ii.U >= cycsSinceDone.io.output.data) & (io.input.numIter != 0.U(numIterWidth.W)) & io.input.stageMask(ii)
          }
          io.output.stageEnable.drop(fillStateID+1).foreach { en => en := false.B }
          val doneMaskInts = doneMask.zip(io.input.stageMask).zipWithIndex.map{case ((a,b),iii) => (a | ~b) & iii.U >= cycsSinceDone.io.output.data}.take(fillStateID+1).map {Mux(_, 1.U(bitsToAddress(n).W), 0.U(bitsToAddress(n).W))}
          val doneTree = doneMaskInts.reduce {_ + _} + cycsSinceDone.io.output.data === (fillStateID+1).U
          // val doneTree = doneMask.zipWithIndex.map{case (a,i) => a | ~io.input.stageMask(i)}.take(fillStateID+1).reduce {_ & _}
          doneClear := doneTree

          when (doneTree === 1.U) {
            if (i+1 == steadyState) { // If moving to steady state
              cycsSinceDone.io.input(0).enable := false.B
              stateFF.io.input(0).data := Mux(cycsSinceDone.io.output.data === 0.U & ctr.io.output.count(0) + 1.S < max.asSInt , 
                          steadyState.U, 
                          Mux(io.input.forever, steadyState.U, cycsSinceDone.io.output.data + 2.U + stateFF.io.output.data) 
                        ) // If already in drain step, bypass steady state
              deadState.io.input.set := true.B
            } else {
              cycsSinceDone.io.input(0).data := cycsSinceDone.io.output.data + 1.U
              cycsSinceDone.io.input(0).enable := ctr.io.output.count(0) + 1.S === max.asSInt
              deadState.io.input.set := true.B
              stateFF.io.input(0).data := (i+1).U
            }
          }.otherwise {
            cycsSinceDone.io.input(0).enable := false.B
            stateFF.io.input(0).data := state
            deadState.io.input.set := false.B
          }
        }
      }
    }.elsewhen (state === steadyState.U) {  // STEADY
      io.output.stageEnable.zipWithIndex.foreach { case (en, i) => en := ~doneMask(i) & io.input.stageMask(i)  }
      cycsSinceDone.io.input(0).enable := false.B

      val doneTree = doneMask.zipWithIndex.map{case (a,i) => a | ~io.input.stageMask(i)}.reduce{_ & _}
      doneClear := doneTree
      when (doneTree === 1.U) {
        when(getRetimed(ctr.io.output.count(0),1) === (max.asSInt - 1.S)) {
          stateFF.io.input(0).data := Mux(io.input.forever, steadyState.U, drainState.U)
          deadState.io.input.set := true.B
        }.otherwise {
          stateFF.io.input(0).data := state
          deadState.io.input.set := false.B
        }
      }.otherwise {
        stateFF.io.input(0).data := state
        deadState.io.input.set := false.B
      }
    }.elsewhen (state < doneState.U) {   // DRAIN
      cycsSinceDone.io.input(0).enable := false.B

      for ( i <- drainState until doneState) {
        val drainStateID = i - drainState
        when (state === i.U) {
          io.output.stageEnable.zipWithIndex.takeRight(n - drainStateID - 1).foreach { case (en, i) => en := ~doneMask(i) & io.input.stageMask(i) }
          io.output.stageEnable.dropRight(n - drainStateID - 1).foreach { en => en := 0.U }
          val doneTree = doneMask.zipWithIndex.map{case (a,i) => a | ~io.input.stageMask(i)}.takeRight(n - drainStateID - 1).reduce {_&_}
          doneClear := doneTree
          when (doneTree === 1.U) {
            stateFF.io.input(0).data := (i+1).U
            deadState.io.input.set := true.B
          }.otherwise {
            stateFF.io.input(0).data := state
            deadState.io.input.set := false.B
          }
        }
      }
    }.elsewhen (state === doneState.U) {  // DONE
      io.output.stageEnable.foreach { s => s := false.B}
      doneClear := false.B
      stateFF.io.input(0).data := resetState.U
      deadState.io.input.set := false.B
    }.otherwise {
      io.output.stageEnable.foreach { s => s := false.B}
      stateFF.io.input(0).data := state
      deadState.io.input.set := false.B
    }
  // }.elsewhen(deadState.io.output.data){
  //   deadState.io.input.set := false.B
  //   deadState.io.input.reset := true.B
  //   // io.output.stageEnable.foreach { s => s := false.B}
  //   stateFF.io.input(0).data := state
  //   cycsSinceDone.io.input(0).enable := false.B
  }.otherwise {
    cycsSinceDone.io.input(0).enable := false.B
    deadState.io.input.set := false.B
    deadState.io.input.reset := true.B
    (0 until n).foreach { i => io.output.stageEnable(i) := false.B }
    doneClear := false.B
    stateFF.io.input(0).data := resetState.U
  }

  // Output logic
  io.output.ctr_inc := io.input.stageDone(0) & Utils.delay(~io.input.stageDone(0), 1) // on rising edge
  io.output.done := state === doneState.U //& deadState.io.output.data
  io.output.state := state.asSInt
}


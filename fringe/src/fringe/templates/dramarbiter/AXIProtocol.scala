package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

import fringe._
import fringe.globals._

class AXICmdSplit(dram: DRAMStream) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(dram.cloneType)
    val out = dram.cloneType
  })

  io.out <> io.in

  // split commands if they're larger than AXI supports
  val cmdSizeCounter = Module(new Counter(32))

  val cmdSizeRemaining = io.in.cmd.bits.size - cmdSizeCounter.io.out
  val maxSize = target.maxBurstsPerCmd.U
  // this is the last command within a split command
  val lastCmd = (cmdSizeRemaining <= maxSize)
  val cmdSize = Mux(lastCmd, cmdSizeRemaining, maxSize)
  io.out.cmd.bits.size := cmdSize

  val addrOffsetBytes = cmdSizeCounter.io.out << log2Ceil(target.burstSizeBytes)
  val cmdAddr = DRAMAddress(io.in.cmd.bits.addr + addrOffsetBytes)
  io.out.cmd.bits.addr := cmdAddr.burstAddr
  io.out.cmd.bits.rawAddr := cmdAddr.bits
  val cmdTag = io.in.cmd.bits.getTag
  // tag only the last burst if we split the command, so we can send only the last wresp to the app
  cmdTag.cmdSplitLast := lastCmd
  io.out.cmd.bits.setTag(cmdTag)

  val cmdIssue = io.out.cmd.valid & io.out.cmd.ready
  val cmdDeq = lastCmd & cmdIssue
  io.in.cmd.ready := cmdDeq

  cmdSizeCounter.io.reset := cmdDeq
  cmdSizeCounter.io.enable := cmdIssue
  cmdSizeCounter.io.stride := target.maxBurstsPerCmd.U

  // consolidate write responses if we split a command
  val wrespSplit = io.out.wresp.bits.getTag.cmdSplitLast
  io.in.wresp.valid := io.out.wresp.valid & wrespSplit
  io.out.wresp.ready := Mux(wrespSplit, io.in.wresp.ready, true.B)
}

class AXICmdIssue(dram: DRAMStream) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(dram.cloneType)
    val out = dram.cloneType
  })

  io.out <> io.in

  // track wdata issues within a split command so we know when to send wlast
  val wdataCounter = Module(new Counter(32))
  // issue write commands only once even if we need to issue multiple wdata
  val writeIssued = RegInit(false.B)

  val dramCmdIssue = io.out.cmd.valid & io.out.cmd.ready
  val dramWriteIssue = io.out.wdata.valid & io.out.wdata.ready

  val writeCmd = io.in.cmd.bits.isWr

  val wlast = dramWriteIssue & (wdataCounter.io.next === io.in.cmd.bits.size)
  val cmdDone = Mux(writeCmd, wlast, dramCmdIssue)
  when(wlast) {
    writeIssued := false.B
  } .elsewhen(dramCmdIssue & writeCmd) {
    writeIssued := true.B
  }

  wdataCounter.io.reset := wlast
  wdataCounter.io.enable := dramWriteIssue
  wdataCounter.io.stride := 1.U

  // keep commands queued until writes have been issued
  io.in.cmd.ready := Mux(writeCmd, wlast, dramCmdIssue)
  io.in.wdata.ready := dramWriteIssue

//  io.out.cmd.valid := io.in.cmd.valid & Mux(writeCmd, !writeIssued, true.B)
  io.out.cmd.valid := io.in.cmd.valid
  // wdata valid related to this one?
//  io.out.wdata.valid := io.in.wdata.valid & writeIssued
  io.out.wdata.valid := io.in.wdata.valid
  io.out.wdata.bits.wlast := wlast
}


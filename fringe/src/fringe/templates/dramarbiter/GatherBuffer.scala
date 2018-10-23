package fringe.templates.dramarbiter

import chisel3._
import chisel3.util._

import fringe._
import fringe.globals._

// find which addresses overlap within the gather vector
class GatherAddressSelector(
  val v: Int
) extends Module {

  val io = IO(new Bundle {
    val in = Vec(v, Flipped(Decoupled(new DRAMAddress)))
    val out = Decoupled(new DRAMAddress)
  })

  // keep track of which lanes have issued
  val issueMaskOut = List.tabulate(v) { i =>
    val reg = RegInit(false.B)
    reg
  }

  // we issue loads out of order so force priority on unissued lanes to avoid starving them
  val issueIdx = PriorityEncoder(issueMaskOut.map { ~_ })
  val issueCmd = io.in(issueIdx)

  val issue = io.out.valid & io.out.ready

  val issueMaskIn = io.in.map { case in =>
    issue & in.valid & (in.bits.burstTag === issueCmd.bits.burstTag)
  }
  val issueDone = issueMaskOut.zip(issueMaskIn).map { case (a, b) => a | b }.reduce { _&_ }

  issueMaskOut.zipWithIndex.foreach { case (reg, i) =>
    when (issue) {
      reg := Mux(issueDone, false.B, issueMaskIn(i))
    }
  }

  io.in.zipWithIndex.foreach { case (in, i) =>
    in.ready := issueMaskIn(i)
  }
  io.out.valid := io.in.map { _.valid }.reduce { _|_ }
  io.out.bits := issueCmd.bits
}

class GatherBuffer(
  val w: Int,
  val v: Int,
  val depth: Int
) extends Module {

  class MetaData extends Bundle {
    val done = Bool()
    val word = UInt(w.W)
    val addr = new DRAMAddress

    override def cloneType(): this.type = new MetaData().asInstanceOf[this.type]
  }

  val io = IO(new Bundle {

    class ReadResp extends Bundle {
      val burstTag = UInt(32.W)
      val data = Vec(EXTERNAL_W * EXTERNAL_V / w, UInt(w.W))

      override def cloneType(): this.type = new ReadResp().asInstanceOf[this.type]
    }

    val cmdAddr = Vec(v, Flipped(Decoupled(new DRAMAddress)))
    val issueAddr = Decoupled(new DRAMAddress)
    val rresp = Flipped(Decoupled(new ReadResp))
    val rdata = Decoupled(Vec(v, UInt(w.W)))
  })

  val meta = List.fill(v) { Module(new FIFO(new MetaData, depth / v, true)) }

  val addressSel = Module(new GatherAddressSelector(v))
  addressSel.io.in <> io.cmdAddr

  val issueAddr = addressSel.io.out.bits
  
  meta.zipWithIndex.foreach { case (fifo, i) =>
    fifo.io.in.valid := addressSel.io.in(i).valid & addressSel.io.in(i).ready
    fifo.io.in.bits.done := false.B
    fifo.io.in.bits.addr := addressSel.io.in(i).bits
    fifo.io.out.ready := io.rdata.valid & io.rdata.ready

    fifo.io.banks.foreach { case bank =>
      val rrespHit = bank.rdata.valid & !bank.rdata.bits.done & io.rresp.valid &
                     (bank.rdata.bits.addr.burstTag === io.rresp.bits.burstTag)
      bank.wdata.valid := rrespHit
      bank.wdata.bits.done := true.B
      bank.wdata.bits.word := io.rresp.bits.data(bank.rdata.bits.addr.wordOffset(w))
    }
  }

  val issueHit = meta.map { case fifo =>
    fifo.io.banks.map { case bank =>
      val bankHit = bank.rdata.valid & !bank.rdata.bits.done &
                    addressSel.io.out.valid & (bank.rdata.bits.addr.burstTag === issueAddr.burstTag)
      bankHit
    }.reduce { _|_ }
  }.reduce { _|_ }

  io.issueAddr.valid := addressSel.io.out.valid & !issueHit
  io.issueAddr.bits := issueAddr
  val deqIssueAddr = Mux(issueHit, true.B, io.issueAddr.ready) & meta.map { _.io.in.ready }.reduce { _&_ }
  addressSel.io.out.ready := deqIssueAddr

  io.rdata.valid := meta.map { case fifo =>
    fifo.io.out.valid & fifo.io.out.bits.done
  }.reduce { _&_ }
  io.rdata.bits := Vec(meta.map { _.io.out.bits.word })

  io.rresp.ready := true.B

}

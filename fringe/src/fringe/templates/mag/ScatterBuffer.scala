package fringe.templates.mag

import chisel3._
import chisel3.util.{Valid,UIntToOH}
import fringe._
import fringe.templates.memory.{FIFOCore,FIFOBaseIO}
import fringe.utils.vecWidthConvert

class ScatterBuffer(
  val streamW: Int,
  val d: Int,
  val streamV: Int,
  val burstSize: Int,
  val addrWidth: Int,
  val sizeWidth: Int,
  val readResp: DRAMReadResponse
) extends Module {
  class MetaData extends Bundle {
    val valid = Bool()

    override def cloneType(): this.type = new MetaData().asInstanceOf[this.type]
  }

  class ScatterData extends Bundle {
    val data = UInt(streamW.W)
    val meta = new MetaData

    override def cloneType(): this.type = new ScatterData().asInstanceOf[this.type]
  }

  val countWidth = 16
  val v = readResp.rdata.getWidth / streamW

  val fData = Module(new FIFOCore(new ScatterData, d, v, true))
  fData.io.config.chainRead := false.B
  fData.io.config.chainWrite := false.B
  val fCmd = Module(new FIFOCore(new Command(addrWidth, sizeWidth, 0), fData.bankSize, 1, true))
  fCmd.io.config.chainRead := false.B
  fCmd.io.config.chainWrite := false.B
  val fCount = Module(new FIFOCore(UInt(countWidth.W), fData.bankSize, 1, true))
  fCount.io.config.chainRead := false.B
  fCount.io.config.chainWrite := false.B

  class ScatterBufferIO extends Bundle {
    class WData extends Bundle {
      val data = Vec(v, UInt(streamW.W))
      val count = UInt(countWidth.W)
      val cmd = new Command(addrWidth, sizeWidth, 0)

      override def cloneType(): this.type = new WData().asInstanceOf[this.type]
    }

    val fifo = new FIFOBaseIO(new WData, fData.bankSize, 1)
    val rresp = Input(Valid(readResp))
    val hit = Output(Bool())
    val complete = Output(Bool())
  }
  
  val io = IO(new ScatterBufferIO)
  
  val cmdAddr = Wire(new BurstAddr(addrWidth, streamW, burstSize))
  cmdAddr.bits := io.fifo.enq(0).cmd.addr

  fData.io.enq.zipWithIndex.foreach { case (e, i) =>
    e.meta.valid := (cmdAddr.wordOffset === i.U)
    e.data := io.fifo.enq(0).data(i)
  }
  fCmd.io.enq(0) := io.fifo.enq(0).cmd
  fCount.io.enq(0) := 1.U

  val enqVld = io.fifo.enqVld & ~io.hit
  fData.io.enqVld := enqVld
  fCmd.io.enqVld := enqVld
  fCount.io.enqVld := enqVld
  val (issueHits, respHits) = fCmd.io.banks match {
    case Some(b) =>
      b.zipWithIndex.map { case (bank, i) => 
        val addr = Wire(new BurstAddr(addrWidth, streamW, burstSize))
        addr.bits := bank(0).rdata.addr
        val valid = bank(0).valid
        val issueHit = valid & (addr.burstTag === cmdAddr.burstTag)
        val respHit = valid & (addr.burstTag === io.rresp.bits.tag.uid) & io.rresp.valid
        (issueHit, respHit)
      }.unzip
    case None => throw new Exception
  }
  io.hit := issueHits.reduce { _|_ }

  fData.io.banks match {
    case Some(b) =>
      b.zipWithIndex.foreach { case (bank, i) => 
        val count = fCount.io.banks.get.apply(i).apply(0)
        val wen = issueHits(i) & io.fifo.enqVld
        count.wen := wen
        count.wdata := count.rdata + 1.U
        bank.zipWithIndex.foreach { case (d, j) =>
          val writeWord = UIntToOH(cmdAddr.wordOffset)(j) & wen
          val writeResp = respHits(i) & !d.rdata.meta.valid
          d.wen := writeWord | writeResp
          val rdata = vecWidthConvert(io.rresp.bits.rdata, streamW)
          d.wdata.data := Mux(writeWord, io.fifo.enq(0).data(j), rdata(j))
          d.wdata.meta.valid := true.B
        }
      }
    case None => throw new Exception
  }

  io.fifo.deq(0).data := fData.io.deq.map { _.data }
  io.fifo.deq(0).cmd := fCmd.io.deq(0)
  io.fifo.deq(0).count := fCount.io.deq(0)
  io.fifo.full := fData.io.full
  io.complete := fData.io.deq.map { _.meta.valid }.reduce { _&_ } & !fData.io.empty
  io.fifo.empty := fData.io.empty
  

  val deqVld = io.fifo.deqVld & io.complete
  fData.io.deqVld := deqVld
  fCmd.io.deqVld := deqVld
  fCount.io.deqVld := deqVld
}

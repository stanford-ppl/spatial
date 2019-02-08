package fringe.templates.memory

import chisel3._
import fringe._
import fringe.templates.counters.{NBufCtr, SingleCounter}
import fringe.Ledger._
import fringe.globals
import fringe.templates.math.FixedPoint
import fringe.utils.{getRetimed, log2Up, risingEdge}
import fringe.utils.HVec
import fringe.utils._
import fringe.utils.XMap._
import fringe.utils.DMap._
import fringe.utils.NBufDMap._
import fringe.utils.NBufXMap._
import fringe.utils.implicits._

/* Controller that is instantiated in NBuf templates to handle port -> module muxing */
class NBufController(numBufs: Int, portsWithWriter: List[Int]) extends Module {

  val io = IO( new Bundle {
    val sEn       = Vec(numBufs, Input(Bool()))
    val sDone     = Vec(numBufs, Input(Bool()))
    val statesInW = Vec(1 max portsWithWriter.distinct.length, Output(UInt((1+log2Up(numBufs)).W)))
    val statesInR = Vec(numBufs, Output(UInt((1+log2Up(numBufs)).W)))
    val swap      = Output(Bool())
  })

  // Logic for recognizing state swapping
  val sEn_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val swap = Wire(Bool())
  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numBufs).foreach{ i => 
    sEn_latch(i).io.input.set := io.sEn(i) & !io.sDone(i)
    sEn_latch(i).io.input.reset := getRetimed(swap,1)
    sEn_latch(i).io.input.asyn_reset := getRetimed(reset, 1)
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := getRetimed(swap,1)
    sDone_latch(i).io.input.asyn_reset := getRetimed(reset, 1)
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output }.reduce{_|_}
  swap := risingEdge(sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done), i) => en.io.output === (done.io.output || io.sDone(i)) }.reduce{_&_} & anyEnabled)
  io.swap := swap

  // Counters for reporting writer and reader buffer pointers
  // Mapping input write ports to their appropriate bank
  val statesInW = portsWithWriter.distinct.sorted.zipWithIndex.map { case (t,i) =>
    val c = Module(new NBufCtr(1,Some(t), Some(numBufs), 1+log2Up(numBufs)))
    c.io <> DontCare
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInW(i) := c.io.output.count
    (t -> c)
  }

  // Mapping input read ports to their appropriate bank
  val statesInR = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr(1,Some(i), Some(numBufs), 1+log2Up(numBufs)))
    c.io <> DontCare
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInR(i) := c.io.output.count
    c
  }

  def lookup(id: Int): Int = { portsWithWriter.sorted.distinct.indexOf(id) }

}


class NBufMem(np: NBufParams) extends Module {

  def this( mem: MemType, logicalDims: List[Int], 
    numBufs: Int, bitWidth: Int, banks: List[Int], strides: List[Int], 
    WMapping: List[Access], RMapping: List[Access],
    bankingMode: BankingMode, inits: Option[List[Double]] = None, syncMem: Boolean = false, fracBits: Int = 0, numActives: Int = 1, myName: String = "NBuf"
  ) = this(NBufParams(numBufs, mem, MemParams(FIFOInterfaceType, logicalDims,bitWidth,banks,strides,WMapping,RMapping,bankingMode,inits,syncMem,fracBits,true,numActives,myName)))

  override def desiredName = np.p.myName

  val io = IO( new NBufInterface(np) )

  io.accessActivesOut.zip(io.accessActivesIn).foreach{case (o,i) => o := i}
  io.full := DontCare
  io.almostFull := DontCare
  io.empty := DontCare
  io.almostEmpty := DontCare
  io.numel := DontCare

  // Instantiate buffer controller
  val ctrl = Module(new NBufController(np.numBufs, np.portsWithWriter))
  for (i <- 0 until np.numBufs){
    ctrl.io.sEn(i) := io.sEn(i)
    ctrl.io.sDone(i) := io.sDone(i)
  }


  // Create physical mems
  np.mem match {
    case BankedSRAMType => 
      val srams = (0 until np.numBufs).map{ i => 
        val x = Module(new BankedSRAM(np.p.logicalDims, np.p.bitWidth,
                        np.p.banks, np.p.strides, 
                        np.p.WMapping, np.p.RMapping,
                        np.p.bankingMode, np.p.inits, np.p.syncMem, np.p.fracBits, np.p.numActives, "SRAM"))
        x.io <> DontCare
        x
      }
      // Route NBuf IO to SRAM IOs
      srams.zipWithIndex.foreach{ case (f,i) => 
        np.p.WMapping.zipWithIndex.foreach { case (a,j) =>
          val wMask = if (a.port.bufPort.isDefined) {ctrl.io.statesInW(ctrl.lookup(a.port.bufPort.get)) === i.U} else true.B
          f.connectBufW(io.wPort(j), j, wMask)
        }
        np.p.RMapping.zipWithIndex.foreach { case (a,j) =>
          val rMask = if (a.port.bufPort.isDefined) {ctrl.io.statesInR(a.port.bufPort.get) === i.U} else true.B
          f.connectBufR(io.rPort(j), j, rMask)
        }
      }

      np.p.RMapping.zipWithIndex.collect{case (p, i) if (p.port.bufPort.isDefined) => 
        val outSel = (0 until np.numBufs).map{ a => ctrl.io.statesInR(p.port.bufPort.get) === a.U }
        io.rPort(i).output.zipWithIndex.foreach{case (r, j) => r := chisel3.util.Mux1H(outSel, srams.map{f => f.io.rPort(i).output(j)})}
      }


    case FFType => 
      val ffs = (0 until np.numBufs).map{ i => 
        val x = Module(new FF(np.p.bitWidth, np.p.WMapping, np.p.RMapping, np.p.inits, np.p.fracBits, numActives = 1, "FF")) 
        x.io <> DontCare
        x
      }
      ffs.foreach(_.io.reset := io.reset)
      ffs.zipWithIndex.foreach{ case (f,i) => 
        np.p.WMapping.zipWithIndex.foreach { case (a,j) =>
          val wMask = if (a.port.bufPort.isDefined) {ctrl.io.statesInW(ctrl.lookup(a.port.bufPort.get)) === i.U} else true.B
          f.connectBufW(io.wPort(j), j, wMask)
        }
        np.p.RMapping.zipWithIndex.foreach { case (a,j) =>
          val rMask = if (a.port.bufPort.isDefined) {ctrl.io.statesInR(a.port.bufPort.get) === i.U} else true.B
          f.connectBufR(io.rPort(j), j, rMask)
        }
      }
      np.p.RMapping.zipWithIndex.collect{case (p, i) if (p.port.bufPort.isDefined) => 
        val outSel = (0 until np.numBufs).map{ a => ctrl.io.statesInR(p.port.bufPort.get) === a.U }
        io.rPort(i).output.zipWithIndex.foreach{case (r, j) => r := chisel3.util.Mux1H(outSel, ffs.map{f => f.io.rPort(i).output(j)})}
      }

    case FIFORegType => throw new Exception("NBuffered FIFOReg should be impossible?")

    case FIFOType => throw new Exception("NBuffered FIFO should be impossible?")

    case ShiftRegFileType => 
      def posMod(n: Int, d: Int): Int = ((n % d) + d) % d
      val isShift = np.p.WMapping.exists(_.shiftAxis.isDefined)
      var shiftEntryBuf: Option[Int] = None
      val rfs = (0 until np.numBufs).map{ i => 
        val isShiftEntry = isShift && np.p.WMapping.filter(_.port.bufPort == Some(i)).nonEmpty
        if (isShiftEntry) shiftEntryBuf = Some(i)
        val WPorts = np.p.WMapping.filter{x => x.port.bufPort == Some(i) || !x.port.bufPort.isDefined}
        val RPorts = np.p.RMapping.filter(_.port.bufPort == Some(i))
        val x = Module(new ShiftRegFile(np.p.logicalDims, np.p.bitWidth, np.p.logicalDims, np.p.logicalDims,
                        if (WPorts.isEmpty) List(AccessHelper.singular(np.p.bitWidth)) else WPorts, if (RPorts.isEmpty) List(AccessHelper.singular(np.p.bitWidth)) else RPorts,
                        np.p.inits, np.p.syncMem, np.p.fracBits, isBuf = !isShiftEntry, np.p.numActives, "sr"))
        x.io.asInstanceOf[ShiftRegFileInterface] <> DontCare
        x
      }
      rfs.zipWithIndex.foreach{case (rf, i) => 
        if (!shiftEntryBuf.exists(_ == i)) {
          rf.io.asInstanceOf[ShiftRegFileInterface].dump_in.zip(rfs(posMod((i-1),np.numBufs)).io.asInstanceOf[ShiftRegFileInterface].dump_out).foreach{
            case(a,b) => a:=b
          }
          rf.io.asInstanceOf[ShiftRegFileInterface].dump_en := ctrl.io.swap
        }
      }
      rfs.foreach(_.io.reset := io.reset)

      // Route NBuf IO to SRAM IOs
      rfs.zipWithIndex.foreach{ case (f,i) => 
        val bufWPorts = np.p.WMapping.zip(io.wPort).collect{case (x,p) if (x.port.bufPort == Some(i) || !x.port.bufPort.isDefined) => p}
        val bufRPorts = np.p.RMapping.zip(io.rPort).collect{case (x,p) if (x.port.bufPort == Some(i)) => p}
        f.io.wPort.zip(bufWPorts).foreach{case (a,b) => a <> b}
        f.io.rPort.zip(bufRPorts).foreach{case (a,b) => a <> b}
      }

    case LineBufferType => 
      val rowstride = np.p.strides(0)
      val numrows = np.p.logicalDims(0) + (np.numBufs-1)*rowstride
      val numcols = np.p.logicalDims(1)
      val lb = Module(new BankedSRAM(List(numrows,numcols), np.p.bitWidth,
                               List(numrows,np.p.banks(1)), np.p.strides,
                               np.p.WMapping.map(_.randomBanks), np.p.RMapping.map(_.randomBanks),
                               np.p.bankingMode, np.p.inits, np.p.syncMem, np.p.fracBits, numActives = np.p.numActives, "lb"))
      lb.io <> DontCare
      val numWriters = np.p.WMapping.map(_.par).sum
      val par = np.p.WMapping.map(_.par).max
      val writeCol = Module(new SingleCounter(par, Some(0), Some(numcols), Some(1), false))
      writeCol.io <> DontCare
      val en = io.wPort.flatMap(_.en).reduce{_||_}
      writeCol.io.input.enable := en
      writeCol.io.input.reset := ctrl.io.swap
      writeCol.io.setup.saturate := false.B

      val rowChanged = io.wPort.map{p => getRetimed(p.banks(0),1) =/= p.banks(0)}.reduce{_||_}
      val gotFirstInRow = Module(new SRFF())
      gotFirstInRow.io.input.set := risingEdge(en) || (en && rowChanged)
      gotFirstInRow.io.input.reset :=  rowChanged | ctrl.io.swap
      gotFirstInRow.io.input.asyn_reset := false.B

      val base = chisel3.util.PriorityMux(io.wPort.flatMap(_.en), writeCol.io.output.count)
      val colCorrection = Module(new FF(32))
      colCorrection.io <> DontCare
      colCorrection.io.wPort(0).data.head := base.asUInt
      colCorrection.io.wPort(0).init := 0.U
      colCorrection.io.wPort(0).en.head := en & ~gotFirstInRow.io.output
      colCorrection.io.wPort(0).reset := reset.toBool | risingEdge(!gotFirstInRow.io.output)
      val colCorrectionValue = Mux(en & ~gotFirstInRow.io.output, base.asUInt, colCorrection.io.rPort(0).output.head)

      val wCRN_width = 1 + log2Up(numrows)
      val writeRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), 0, wCRN_width))
      writeRow.io <> DontCare
      writeRow.io.input.enable := ctrl.io.swap
      writeRow.io.input.countUp := false.B

      // Connect wPorts
      lb.io.wPort.zip(io.wPort).foreach { case (mem, nbuf) =>
        mem.en := nbuf.en
        mem.data := nbuf.data
        mem.ofs := writeCol.io.output.count.map{x => (x.asUInt - colCorrectionValue) / np.p.banks(1).U}
        mem.banks.zipWithIndex.map{case (b,i) => 
          if (i % 2 == 0) b := writeRow.io.output.count + (rowstride-1).U - nbuf.banks(0)
          else b := (writeCol.io.output.count(i/2).asUInt - colCorrectionValue) % np.p.banks(1).U
        }
      }

      val readRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), (np.numBufs-1)*rowstride, wCRN_width))
      readRow.io <> DontCare
      readRow.io.input.enable := ctrl.io.swap
      readRow.io.input.countUp := false.B

      // Connect rPorts and the associated outputs
      lb.io.rPort.zip(io.rPort).foreach { case (mem, nbuf) =>
        mem.en := nbuf.en
        mem.backpressure := nbuf.backpressure
        nbuf.output := mem.output
        mem.ofs := nbuf.ofs
        mem.banks.odd.zip(nbuf.banks.odd).foreach{case (a: UInt, b:UInt) => a := b}
        mem.banks.even.zip(nbuf.banks.even).foreach{case (a:UInt,b:UInt) => a := (b + readRow.io.output.count) % numrows.U}
      }

    case LIFOType => throw new Exception("NBuffered LIFO should be impossible?")
  }
}



class RegChainPass(val numBufs: Int, val bitWidth: Int, myName: String = "") extends Module { 
  val wMap = List(AccessHelper.singularOnPort(0, bitWidth))
  val rMap = List.tabulate(numBufs-1){i => AccessHelper.singularOnPort(i+1, bitWidth)}
  val io = IO( new NBufInterface(NBufParams(numBufs, FFType, MemParams(FIFOInterfaceType, List(1), bitWidth, List(1), List(1), wMap, rMap, BankedMemory))))

  override def desiredName = myName
  
  io.accessActivesOut(0) := io.accessActivesIn(0)
  io.full := DontCare
  io.almostFull := DontCare
  io.empty := DontCare
  io.almostEmpty := DontCare
  io.numel := DontCare

  val nbufFF = Module(new NBufMem(FFType, List(1), numBufs, bitWidth, List(1), List(1), wMap, rMap, BankedMemory))
  io <> nbufFF.io

  def connectStageCtrl(done: Bool, en: Bool, port: Int): Unit = {
    io.sEn(port) := en
    io.sDone(port) := done
  }

  def chain_pass[T](dat: T, en: Bool): Unit = { // Method specifically for handling reg chains that pass counter values between metapipe stages
    dat match {
      case data: UInt       => io.wPort(0).data.head := data
      case data: FixedPoint => io.wPort(0).data.head := data.number
    }
    io.wPort(0).en.head := en
    io.wPort(0).reset := getRetimed(reset, 1)
    io.wPort(0).init := 0.U
  }


  def read(i: Int): UInt = {
    io.rPort(i-1).output.head
  }


}
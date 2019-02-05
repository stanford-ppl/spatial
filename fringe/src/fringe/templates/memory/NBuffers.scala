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
//       val fifo = Module(new FIFO(List(p.logicalDims.head), p.bitWidth, 
//                                   p.banks, p.combinedXBarWMux, p.combinedXBarRMux, p.numActives))
//       fifo.io.asInstanceOf[FIFOInterface] <> DontCare

//       fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < p.numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-p.numXBarW)}
//       fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < p.numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-p.numXBarR)}
//       p.combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
//         val base = p.combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2, muxAddr._3).sum
//         (0 until entry._1).foreach{i => io.output(base + i) := fifo.io.output(i)}
//       }
//       io.full := fifo.io.asInstanceOf[FIFOInterface].full
//       io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
//       io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
//       io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
//       io.numel := fifo.io.asInstanceOf[FIFOInterface].numel

    case ShiftRegFileType => 
//       def posMod(n: Int, d: Int): Int = ((n % d) + d) % d
//       val isShift = p.xBarWMux.mergeXMaps.values.map(_._2).exists(_.isDefined)
//       var shiftEntryBuf: Option[Int] = None
//       val rfs = (0 until p.numBufs).map{ i => 
//         val combinedXBarWMux = p.xBarWMux.getOrElse(i,XMap()).merge(p.broadcastWMux)
//         val combinedXBarRMux = p.xBarRMux.getOrElse(i,XMap()).merge(p.broadcastRMux)
//         val isShiftEntry = isShift && combinedXBarWMux.nonEmpty
//         if (isShiftEntry) shiftEntryBuf = Some(i)
//         val x = Module(new ShiftRegFile(p.logicalDims, p.bitWidth, 
//                         combinedXBarWMux, combinedXBarRMux,
//                         p.directWMux.getOrElse(i, DMap()), p.directRMux.getOrElse(i,DMap()),
//                         p.inits, p.syncMem, p.fracBits, isBuf = !isShiftEntry, p.numActives, "sr"))
//         x.io.asInstanceOf[ShiftRegFileInterface] <> DontCare
//         x
//       }
//       rfs.zipWithIndex.foreach{case (rf, i) => 
//         if (!shiftEntryBuf.exists(_ == i)) {
//           rf.io.asInstanceOf[ShiftRegFileInterface].dump_in.zip(rfs(posMod((i-1),p.numBufs)).io.asInstanceOf[ShiftRegFileInterface].dump_out).foreach{
//             case(a,b) => a:=b
//           }
//           rf.io.asInstanceOf[ShiftRegFileInterface].dump_en := ctrl.io.swap
//         }
//       }
//       rfs.foreach(_.io.reset := io.reset)

//       // Route NBuf IO to SRAM IOs
//       rfs.zipWithIndex.foreach{ case (f,i) => 
//         // Connect XBarW ports
//         if (p.xBarWMux.contains(i)) {
//           val xBarWMuxPortMapping = p.xBarWMux(i)
//           val xBarWMuxBufferBase = p.xBarWMux.accessParsBelowBufferPort(i).length // Index into NBuf io
//           val sramXBarWPorts = xBarWMuxPortMapping.accessPars.length
//           (0 until sramXBarWPorts).foreach {k => 
//             f.io.xBarW(k).en := io.xBarW(xBarWMuxBufferBase + k).en
//             f.io.xBarW(k).shiftEn := io.xBarW(xBarWMuxBufferBase + k).shiftEn
//             f.io.xBarW(k).data := io.xBarW(xBarWMuxBufferBase + k).data
//             f.io.xBarW(k).ofs := io.xBarW(xBarWMuxBufferBase + k).ofs
//             f.io.xBarW(k).banks.zip(io.xBarW(xBarWMuxBufferBase + k).banks).foreach{case (a:UInt,b:UInt) => a := b}
//           }
//         }

//         // Connect DirectW ports
//         if (p.directWMux.contains(i)) {
//           val directWMuxPortMapping = p.directWMux(i)
//           val directWMuxBufferBase = p.directWMux.accessParsBelowBufferPort(i).length // Index into NBuf io
//           val sramDirectWPorts = directWMuxPortMapping.accessPars.length
//           (0 until sramDirectWPorts).foreach {k => 
//             f.io.directW(k).en := io.directW(directWMuxBufferBase + k).en
//             f.io.directW(k).shiftEn := io.directW(directWMuxBufferBase + k).shiftEn
//             f.io.directW(k).data := io.directW(directWMuxBufferBase + k).data
//             f.io.directW(k).ofs := io.directW(directWMuxBufferBase + k).ofs
//           }
//         }

//         // Connect BroadcastW ports
//         if (p.hasBroadcastW) {
//           val sramXBarWBase = if (p.xBarWMux.contains(i)) p.xBarWMux(i).values.size else 0
//           val sramBroadcastWPorts = p.broadcastWMux.accessPars.length
//           (0 until sramBroadcastWPorts).foreach {k => 
//             f.io.xBarW(sramXBarWBase + k).en := io.broadcastW(k).en
//             f.io.xBarW(sramXBarWBase + k).shiftEn := io.broadcastW(k).shiftEn
//             f.io.xBarW(sramXBarWBase + k).data := io.broadcastW(k).data
//             f.io.xBarW(sramXBarWBase + k).ofs := io.broadcastW(k).ofs
//             f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcastW(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
//           }
//         }

//         // Connect BroadcastR ports
//         if (p.hasBroadcastR) {
//           val sramXBarRBase = if (p.xBarRMux.contains(i)) p.xBarRMux(i).values.size else 0
//           val sramBroadcastRPorts = p.broadcastRMux.accessPars.length
//           (0 until sramBroadcastRPorts).foreach {k => 
//             f.io.xBarR(sramXBarRBase + k).en := io.broadcastR(k).en
//             // f.io.xBarR(sramXBarRBase + k).shiftEn := io.broadcastR(k).shiftEn
//             // f.io.xBarR(sramXBarRBase + k).data := io.broadcastR(k).data
//             f.io.xBarR(sramXBarRBase + k).ofs := io.broadcastR(k).ofs
//             f.io.xBarR(sramXBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
//           }
//         }

//         // Connect XBarR ports and the associated outputs
//         if (p.xBarRMux.contains(i)) {
//           val xBarRMuxPortMapping = p.xBarRMux(i)
//           val xBarRMuxBufferBase = p.xBarRMux.accessParsBelowBufferPort(i).length // Index into NBuf io
//           val outputXBarRMuxBufferBase = p.xBarRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
//           val sramXBarRPorts = xBarRMuxPortMapping.accessPars.length
//           (0 until sramXBarRPorts).foreach {k => 
//             val port_width = xBarRMuxPortMapping.accessPars(k)
//             val k_base = xBarRMuxPortMapping.accessPars.take(k).sum
//             (0 until port_width).foreach{m => 
//               val sram_index = (k_base + m) - xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
//               io.output(outputXBarRMuxBufferBase + (k_base + m)) := f.io.output(sram_index)
//             }
//             f.io.xBarR(k).en := io.xBarR(xBarRMuxBufferBase + k).en
//             // f.io.xBarR(xBarRMuxBufferBase + k).data := io.xBarR(xBarRMuxBufferBase + k).data
//             f.io.xBarR(k).ofs := io.xBarR(xBarRMuxBufferBase + k).ofs
//             f.io.xBarR(k).banks.zip(io.xBarR(xBarRMuxBufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
//               // f.io.backpressure(k) := io.backpressure(k) // Dangerous move here
//           }
//         }

//         // Connect DirectR ports and the associated outputs
//         if (p.directRMux.contains(i)) {
//           val directRMuxPortMapping = p.directRMux(i)
//           val directRMuxBufferBase = p.directRMux.accessParsBelowBufferPort(i).length // Index into NBuf io
//           val xBarRBase = p.xBarRMux.accessPars.length
//           val outputDirectRMuxBufferBase = p.directRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
//           val outputXBarRBase = p.xBarRMux.accessPars.sum
//           val sramDirectRPorts = directRMuxPortMapping.accessPars.length
//           (0 until sramDirectRPorts).foreach {k => 
//             val port_width = directRMuxPortMapping.accessPars(k)
//             val k_base = directRMuxPortMapping.accessPars.take(k).sum
//             (0 until port_width).foreach{m => 
//               val sram_index = (k_base + m) - directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
//               io.output(outputXBarRBase + outputDirectRMuxBufferBase + (k_base + m)) := f.io.output(outputXBarRBase + k_base + m)
//             }
//             f.io.directR(k).en := io.directR(directRMuxBufferBase + k).en
//             // f.io.directR(directRMuxBufferBase + k).data := io.directR(directRMuxBufferBase + k).data
//             f.io.directR(k).ofs := io.directR(directRMuxBufferBase + k).ofs
//             // f.io.backpressure(k + {if (hasXBarR) numXBarR else 0}) := io.backpressure(k + {if (hasXBarR) numXBarR else 0}) // Dangerous move here
//           }
//         }

//         // Connect BroadcastR ports and the associated outputs
//         // TODO: Broadcasting sram reads are untested and expected behavior is unclear, currently getting last buffer all the time just because
//         val xBarRMuxPortMapping = p.broadcastRMux
//         val xBarRMuxBufferBase = p.xBarRMux.accessPars.sum
//         val sramXBarRPorts = xBarRMuxPortMapping.accessPars.sum
//         (0 until sramXBarRPorts).foreach {k => 
//           io.output(xBarRMuxBufferBase + k) := f.io.output(k)
//           f.io.xBarR(xBarRMuxBufferBase + k).en := io.broadcastR(k).en
//           f.io.xBarR(xBarRMuxBufferBase + k).ofs := io.broadcastR(k).ofs
//           f.io.xBarR(xBarRMuxBufferBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
//         }
//       }
    case LineBufferType => 
//       val rowstride = p.strides(0)
//       val numrows = p.logicalDims(0) + (p.numBufs-1)*rowstride
//       val numcols = p.logicalDims(1)
//       val combinedXBarWMux = p.xBarWMux.mergeXMaps
//       val combinedXBarRMux = p.xBarRMux.mergeXMaps
//       val flatDirectWMux = p.directWMux.mergeDMaps 
//       val flatDirectRMux = p.directRMux.mergeDMaps 
//       val lb = Module(new BankedSRAM(List(numrows,numcols), p.bitWidth,
//                                List(numrows,p.banks(1)), p.strides,
//                                combinedXBarWMux, combinedXBarRMux,
//                                flatDirectWMux, flatDirectRMux,
//                                p.bankingMode, p.inits, p.syncMem, p.fracBits, numActives = p.numActives, "lb"))
//       lb.io <> DontCare
      
//       val numWriters = p.numXBarW + p.numDirectW
//       val par = combinedXBarWMux.accessPars.sorted.headOption.getOrElse(0) max flatDirectWMux.accessPars.sorted.headOption.getOrElse(0)
//       val writeCol = Module(new SingleCounter(par, Some(0), Some(numcols), Some(1), false))
//       writeCol.io <> DontCare
//       val en = io.xBarW.map(_.en.reduce{_||_}).reduce{_||_} || io.directW.map(_.en.reduce{_||_}).reduce{_||_}
//       writeCol.io.input.enable := en
//       writeCol.io.input.reset := ctrl.io.swap
//       writeCol.io.setup.saturate := false.B

//       val rowChanged = io.xBarW.map{p => getRetimed(p.banks(0),1) =/= p.banks(0)}.reduce{_||_}
//       val gotFirstInRow = Module(new SRFF())
//       gotFirstInRow.io.input.set := risingEdge(en) || (en && rowChanged)
//       gotFirstInRow.io.input.reset :=  rowChanged | ctrl.io.swap
//       gotFirstInRow.io.input.asyn_reset := false.B

//       val base = chisel3.util.PriorityMux(io.xBarW.map(_.en).flatten, writeCol.io.output.count)
//       val colCorrection = Module(new FF(32))
//       colCorrection.io <> DontCare
//       colCorrection.io.xBarW(0).data.head := base.asUInt
//       colCorrection.io.xBarW(0).init.head := 0.U
//       colCorrection.io.xBarW(0).en.head := en & ~gotFirstInRow.io.output
//       colCorrection.io.xBarW(0).reset.head := reset.toBool | risingEdge(!gotFirstInRow.io.output)
//       val colCorrectionValue = Mux(en & ~gotFirstInRow.io.output, base.asUInt, colCorrection.io.output(0))

//       val wCRN_width = 1 + log2Up(numrows)
//       val writeRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), 0, wCRN_width))
//       writeRow.io <> DontCare
//       writeRow.io.input.enable := ctrl.io.swap
//       writeRow.io.input.countUp := false.B

//       // Connect XBarW ports
//       p.xBarWMux.foreach { case (bufferPort, portMapping) =>
//         val bufferBase = p.xBarWMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
//         val sramXBarWPorts = portMapping.accessPars.length
//         // val wMask = getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
//         (0 until sramXBarWPorts).foreach {k => 
//           lb.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en
//           lb.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
//           lb.io.xBarW(bufferBase + k).ofs := writeCol.io.output.count.map{x => (x.asUInt - colCorrectionValue) / p.banks(1).U}
//           lb.io.xBarW(bufferBase + k).banks.zipWithIndex.map{case (b,i) => 
//             if (i % 2 == 0) b := writeRow.io.output.count + (rowstride-1).U - io.xBarW(bufferBase + k).banks(0)
//             else b := (writeCol.io.output.count(i/2).asUInt - colCorrectionValue) % p.banks(1).U
//           }
//         }
//       }

//       val readRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), (p.numBufs-1)*rowstride, wCRN_width))
//       readRow.io <> DontCare
//       readRow.io.input.enable := ctrl.io.swap
//       readRow.io.input.countUp := false.B

//       // Connect XBarR ports and the associated outputs
//       p.xBarRMux.foreach { case (bufferPort, portMapping) =>
//         val bufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
//         val outputBufferBase = p.xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
//         val sramXBarRPorts = portMapping.accessPars.length
//         // val rMask = getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
//         // val outSel = (0 until p.numBufs).map{ a => getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (retime) 1 else 0}) }
//         (0 until sramXBarRPorts).foreach {k => 
//           val port_width = portMapping.sortByMuxPortAndOfs.accessPars(k)
//           val k_base = portMapping.sortByMuxPortAndOfs.accessPars.take(k).sum
//           (0 until port_width).foreach{m => 
//             val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
//             io.output(outputBufferBase + (k_base + m)) := lb.io.output(sram_index)
//           }
//           lb.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en
//           lb.io.xBarR(bufferBase + k).backpressure := io.xBarR(bufferBase + k).backpressure
//           // lb.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
//           lb.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
//           lb.io.xBarR(bufferBase + k).banks.odd.zip(io.xBarR(bufferBase+k).banks.odd).foreach{case (a:UInt,b:UInt) => a := b}
//           lb.io.xBarR(bufferBase + k).banks.even.zip(io.xBarR(bufferBase+k).banks.even).foreach{case (a:UInt,b:UInt) => a := (b + readRow.io.output.count) % numrows.U}
//         }
//       }



    case LIFOType => throw new Exception("NBuffered LIFO should be impossible?")
//       val fifo = Module(new LIFO((List(p.logicalDims.head), p.bitWidth, 
//                                   p.banks, p.combinedXBarWMux, p.combinedXBarRMux, p.numActives)))
//       fifo.io.asInstanceOf[FIFOInterface] <> DontCare
//       fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < p.numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-p.numXBarW)}
//       fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < p.numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-p.numXBarR)}
//       p.combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
//         val base = p.combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2, muxAddr._3).sum
//         (0 until entry._1).foreach{i => io.output(base + i) := fifo.io.output(i)}
//       }
//       io.full := fifo.io.asInstanceOf[FIFOInterface].full
//       io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
//       io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
//       io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
//       io.numel := fifo.io.asInstanceOf[FIFOInterface].numel
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
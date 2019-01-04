package fringe.templates.memory

import chisel3._
import fringe.templates.counters.{NBufCtr, SingleCounter}
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
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  swap := risingEdge(sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done), i) => en.io.output.data === (done.io.output.data || io.sDone(i)) }.reduce{_&_} & anyEnabled)
  io.swap := swap

  // Counters for reporting writer and reader buffer pointers
  // Mapping input write ports to their appropriate bank
  val statesInW = portsWithWriter.distinct.sorted.zipWithIndex.map { case (t,i) =>
    val c = Module(new NBufCtr(1,Some(t), Some(numBufs), 1+log2Up(numBufs)))
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInW(i) := c.io.output.count
    (t -> c)
  }

  // Mapping input read ports to their appropriate bank
  val statesInR = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr(1,Some(i), Some(numBufs), 1+log2Up(numBufs)))
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInR(i) := c.io.output.count
    c
  }

  def lookup(id: Int): Int = { portsWithWriter.sorted.distinct.indexOf(id) }

}


class NBufMem(
  val mem: MemType,
  val logicalDims: List[Int],
  val numBufs: Int,
  val bitWidth: Int,
  val banks: List[Int],
  val strides: List[Int],
  val xBarWMux: NBufXMap,
  val xBarRMux: NBufXMap, // bufferPort -> (muxPort -> accessPar)
  val directWMux: NBufDMap,
  val directRMux: NBufDMap,  // bufferPort -> (muxPort -> List(banks, banks, ...))
  val broadcastWMux: XMap,
  val broadcastRMux: XMap,  // Assume broadcasts are XBar
  val bankingMode: BankingMode,
  val inits: Option[List[Double]] = None,
  val syncMem: Boolean = false,
  val fracBits: Int = 0,
  val myName: String = "NBuf"
) extends Module {

  override def desiredName = myName

  // Overloaded constructers
  // Tuple unpacker
  def this(tuple: (MemType, List[Int], Int, Int, List[Int], List[Int], NBufXMap, NBufXMap, 
    NBufDMap, NBufDMap, XMap, XMap, BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7,tuple._8,tuple._9,tuple._10, tuple._11, tuple._12, tuple._13, None, false, 0)

  val depth = logicalDims.product + {if (mem == LineBufferType) (numBufs-1)*strides(0)*logicalDims(1) else 0} // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = log2Up(depth/banks.product)
  val banksWidths = banks.map(log2Up(_))

  // Compute info required to set up IO interface
  val hasXBarW = xBarWMux.accessPars.sum > 0
  val hasXBarR = xBarRMux.accessPars.sum > 0
  val numXBarW = xBarWMux.accessPars.sum
  val numXBarR = xBarRMux.accessPars.sum
  val numXBarWPorts = xBarWMux.accessPars.size
  val numXBarRPorts = xBarRMux.accessPars.size
  val hasDirectW = directWMux.mergeDMaps.accessPars.sum > 0
  val hasDirectR = directRMux.mergeDMaps.accessPars.sum > 0
  val numDirectW = directWMux.mergeDMaps.accessPars.sum
  val numDirectR = directRMux.mergeDMaps.accessPars.sum
  val numDirectWPorts = directWMux.accessPars.size
  val numDirectRPorts = directRMux.accessPars.size
  val hasBroadcastW = broadcastWMux.accessPars.toList.sum > 0
  val numBroadcastW = broadcastWMux.accessPars.toList.sum
  val numBroadcastWPorts = broadcastWMux.accessPars.toList.length
  val hasBroadcastR = broadcastRMux.accessPars.toList.sum > 0
  val numBroadcastR = broadcastRMux.accessPars.toList.sum
  val numBroadcastRPorts = broadcastRMux.accessPars.toList.length
  val totalOutputs = numXBarR + numDirectR + numBroadcastR
  val defaultDirect = List(List.fill(banks.length)(99))
  val portsWithWriter = (directWMux.keys ++ xBarWMux.keys).toList.sorted

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val xBarW = HVec(Array.tabulate(1 max numXBarWPorts){i => Input(new W_XBar(xBarWMux.accessPars.getOr1(i), ofsWidth, banksWidths, bitWidth))})
    val xBarR = HVec(Array.tabulate(1 max numXBarRPorts){i => Input(new R_XBar(xBarRMux.accessPars.getOr1(i), ofsWidth, banksWidths))}) 
    val directW = HVec(Array.tabulate(1 max numDirectWPorts){i => 
        val dBanks = if (hasDirectW) {
          directWMux.toSeq.sortBy(_._1).toMap.values.flatMap(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).toList(i)
        } else defaultDirect
        Input(new W_Direct(directWMux.accessPars.getOr1(i), ofsWidth, dBanks, bitWidth))
      })
    val directR = HVec(Array.tabulate(1 max numDirectRPorts){i => 
        val dBanks = if (hasDirectR) {
          directRMux.toSeq.sortBy(_._1).toMap.values.flatMap(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).toList(i)
        } else defaultDirect
        Input(new R_Direct(directRMux.accessPars.getOr1(i), ofsWidth, dBanks))
      })
    val broadcastW = HVec(Array.tabulate(1 max numBroadcastWPorts){i => Input(new W_XBar(broadcastWMux.accessPars.getOr1(i), ofsWidth, banksWidths, bitWidth))})
    val broadcastR = HVec(Array.tabulate(1 max numBroadcastRPorts){i => Input(new R_XBar(broadcastRMux.accessPars.getOr1(i), ofsWidth, banksWidths))})
    val reset = Input(Bool())

    // FIFO Specific
    val full = Output(Bool())
    val almostFull = Output(Bool())
    val empty = Output(Bool())
    val almostEmpty = Output(Bool())
    val numel = Output(UInt(32.W))    
    
    val output = new Bundle {
      val data  = Vec(1 max totalOutputs, Output(UInt(bitWidth.W)))  
    }
  })

  // Instantiate buffer controller
  val ctrl = Module(new NBufController(numBufs, portsWithWriter))
  for (i <- 0 until numBufs){
    ctrl.io.sEn(i) := io.sEn(i)
    ctrl.io.sDone(i) := io.sDone(i)
  }

  // Flatten buffer/mux port info and provide each one to each mem
  val flatXBarWMux = xBarWMux.mergeXMaps
  val flatXBarRMux = xBarRMux.mergeXMaps
  val flatDirectWMux = directWMux.mergeDMaps 
  val flatDirectRMux = directRMux.mergeDMaps 
  val combinedXBarWMux = flatXBarWMux.merge(broadcastWMux)
  val combinedXBarRMux = flatXBarRMux.merge(broadcastRMux)

  // Create physical mems
  mem match {
    case BankedSRAMType => 
      val srams = (0 until numBufs).map{ i => 
        Module(new BankedSRAM(logicalDims, bitWidth,
                        banks, strides, 
                        combinedXBarWMux, combinedXBarRMux,
                        flatDirectWMux, flatDirectRMux,
                        bankingMode, inits, syncMem, fracBits, "SRAM"))
      }
      // Route NBuf IO to SRAM IOs
      srams.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.length
          val wMask = getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, 0 /*1*/) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en.map(_ & wMask)
            f.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
            f.io.xBarW(bufferBase + k).ofs := io.xBarW(bufferBase + k).ofs
            f.io.xBarW(bufferBase + k).banks.zip(io.xBarW(bufferBase + k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect DirectW ports
        directWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = directWMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val sramDirectWPorts = portMapping.accessPars.length
          val wMask = getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, 0 /*1*/) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramDirectWPorts).foreach {k => 
            f.io.directW(bufferBase + k).en := io.directW(bufferBase + k).en.map(_ & wMask)
            f.io.directW(bufferBase + k).data := io.directW(bufferBase + k).data
            f.io.directW(bufferBase + k).ofs := io.directW(bufferBase + k).ofs
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.accessPars.length
          val sramBroadcastWPorts = broadcastWMux.accessPars.length
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcastW(k).en
            f.io.xBarW(sramXBarWBase + k).data := io.broadcastW(k).data
            f.io.xBarW(sramXBarWBase + k).ofs := io.broadcastW(k).ofs
            f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcastW(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }
        if (hasBroadcastR) {
          val sramXBarRBase = xBarRMux.accessPars.length
          val sramBroadcastRPorts = broadcastRMux.accessPars.length
          (0 until sramBroadcastRPorts).foreach {k => 
            f.io.xBarR(sramXBarRBase + k).en := io.broadcastR(k).en
            // f.io.xBarR(sramXBarRBase + k).data := io.broadcastR(k).data
            f.io.xBarR(sramXBarRBase + k).ofs := io.broadcastR(k).ofs
            f.io.xBarR(sramXBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        xBarRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val outputBufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarRPorts = portMapping.accessPars.length
          val rMask = getRetimed(ctrl.io.statesInR(bufferPort) === i.U, 0 /*1*/) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR(bufferPort) === a.U, 0 /*1*/) }
          (0 until sramXBarRPorts).foreach {k => 
            val port_width = portMapping.sortByMuxPortAndOfs.accessPars(k)
            val k_base = portMapping.sortByMuxPortAndOfs.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(outputBufferBase + (k_base + m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(sram_index)})
            }
            f.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en.map(_ & rMask)
            f.io.xBarR(bufferBase + k).flow := io.xBarR(bufferBase + k).flow
            // f.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
            f.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
            f.io.xBarR(bufferBase + k).banks.zip(io.xBarR(bufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect DirectR ports and the associated outputs
        directRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val xBarRBase = xBarRMux.accessPars.length
          val outputBufferBase = directRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val outputXBarRBase = xBarRMux.accessPars.sum
          val sramDirectRPorts = portMapping.accessPars.length
          val rMask = getRetimed(ctrl.io.statesInR(bufferPort) === i.U, 0 /*1*/) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR(bufferPort) === a.U, 0 /*1*/) }
          (0 until sramDirectRPorts).foreach {k => 
            val port_width = portMapping.sortByMuxPortAndOfs.accessPars(k)
            val k_base = portMapping.sortByMuxPortAndOfs.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(outputXBarRBase + outputBufferBase + (k_base + m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(sram_index)})
            }

            f.io.directR(bufferBase + k).en := io.directR(bufferBase + k).en.map(_ & rMask)
            f.io.directR(bufferBase + k).flow := io.directR(bufferBase + k).flow
            // f.io.directR(bufferBase + k).data := io.directR(bufferBase + k).data
            f.io.directR(bufferBase + k).ofs := io.directR(bufferBase + k).ofs
          }
        }

        // Connect BroadcastR ports and the associated outputs
        // TODO: Broadcasting sram reads are untested and expected behavior is unclear, currently getting last buffer all the time just because
        val xBarRBase = xBarRMux.accessPars.length
        val directRBase = directRMux.accessPars.length
        val outputXBarRBase = xBarRMux.accessPars.sum
        val outputDirectRBase = directRMux.accessPars.sum
        val sramXBarRPorts = broadcastRMux.accessPars.length
        val outSel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR.head === a.U, 0 /*1*/) }
        (0 until sramXBarRPorts).foreach {k => 
          val port_width = broadcastRMux.accessPars(k)
          val k_base = broadcastRMux.accessPars.take(k).sum
          (0 until port_width).foreach{m => 
            io.output.data(outputXBarRBase + outputDirectRBase + (k_base+m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data((k_base+m))})
          }
         
          f.io.xBarR(xBarRBase + k).en := io.broadcastR( k).en
          // f.io.xBarR(xBarRBase + k).data := io.xBarR(xBarRBase + k).data
          f.io.xBarR(xBarRBase + k).flow := io.broadcastR( k).flow
          f.io.xBarR(xBarRBase + k).ofs := io.broadcastR( k).ofs
          f.io.xBarR(xBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
        }

      }
    case FFType => 
      val ffs = (0 until numBufs).map{ i => 
        Module(new FF(bitWidth, combinedXBarWMux, combinedXBarRMux, inits, fracBits, "FF")) 
      }
      ffs.foreach(_.io.reset := io.reset)
      // Route NBuf IO to FF IOs
      ffs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.sum
          val wMask = getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, 0 /*1*/) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en.map(_ & wMask)
            f.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.accessPars.sum
          val sramBroadcastWPorts = broadcastWMux.accessPars.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcastW(k).en
            f.io.xBarW(sramXBarWBase + k).data := io.broadcastW(k).data
          }
        }
      }

      // Connect buffers to output data ports
      xBarRMux.foreach { case (bufferPort, portMapping) => 
        val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
        val sramXBarRPorts = portMapping.accessPars.sum
        val sel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR(bufferPort) === a.U, 0 /*1*/) }
        (0 until sramXBarRPorts).foreach {k => io.output.data(bufferBase+k) := chisel3.util.Mux1H(sel, ffs.map{f => f.io.output.data(0)})}
      }

      // TODO: BroadcastR connections?
      val xBarRBase = xBarRMux.accessPars.sum
      val sramBroadcastRPorts = broadcastRMux.accessPars.sum
      val outSel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR.head === a.U, 0 /*1*/)}
      (0 until sramBroadcastRPorts).foreach {k => io.output.data(xBarRBase + k) := chisel3.util.Mux1H(outSel, ffs.map{f => f.io.output.data(0)}) }
      
    case FIFORegType => throw new Exception("NBuffered FIFOReg should be impossible?")

    case FIFOType => 
      val fifo = Module(new FIFO(List(logicalDims.head), bitWidth, 
                                  banks, combinedXBarWMux, combinedXBarRMux))

      fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-numXBarW)}
      fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-numXBarR)}
      combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
        val base = combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2, muxAddr._3).sum
        (0 until entry._1).foreach{i => io.output.data(base + i) := fifo.io.output.data(i)}
      }
      io.full := fifo.io.asInstanceOf[FIFOInterface].full
      io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
      io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
      io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
      io.numel := fifo.io.asInstanceOf[FIFOInterface].numel

    case ShiftRegFileType => 
      def posMod(n: Int, d: Int): Int = ((n % d) + d) % d
      val isShift = xBarWMux.mergeXMaps.values.map(_._2).exists(_.isDefined)
      var shiftEntryBuf: Option[Int] = None
      val rfs = (0 until numBufs).map{ i => 
        val combinedXBarWMux = xBarWMux.getOrElse(i,XMap()).merge(broadcastWMux)
        val combinedXBarRMux = xBarRMux.getOrElse(i,XMap()).merge(broadcastRMux)
        val isShiftEntry = isShift && combinedXBarWMux.nonEmpty
        if (isShiftEntry) shiftEntryBuf = Some(i)
        Module(new ShiftRegFile(logicalDims, bitWidth, 
                        combinedXBarWMux, combinedXBarRMux,
                        directWMux.getOrElse(i, DMap()), directRMux.getOrElse(i,DMap()),
                        inits, syncMem, fracBits, isBuf = !isShiftEntry, "sr"))
      }
      rfs.zipWithIndex.foreach{case (rf, i) => 
        if (!shiftEntryBuf.exists(_ == i)) {
          rf.io.asInstanceOf[ShiftRegFileInterface].dump_in.zip(rfs(posMod((i-1),numBufs)).io.asInstanceOf[ShiftRegFileInterface].dump_out).foreach{
            case(a,b) => a:=b
          }
          rf.io.asInstanceOf[ShiftRegFileInterface].dump_en := ctrl.io.swap
        }
      }
      rfs.foreach(_.io.reset := io.reset)

      // Route NBuf IO to SRAM IOs
      rfs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        if (xBarWMux.contains(i)) {
          val xBarWMuxPortMapping = xBarWMux(i)
          val xBarWMuxBufferBase = xBarWMux.accessParsBelowBufferPort(i).length // Index into NBuf io
          val sramXBarWPorts = xBarWMuxPortMapping.accessPars.length
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(k).en := io.xBarW(xBarWMuxBufferBase + k).en
            f.io.xBarW(k).shiftEn := io.xBarW(xBarWMuxBufferBase + k).shiftEn
            f.io.xBarW(k).data := io.xBarW(xBarWMuxBufferBase + k).data
            f.io.xBarW(k).ofs := io.xBarW(xBarWMuxBufferBase + k).ofs
            f.io.xBarW(k).banks.zip(io.xBarW(xBarWMuxBufferBase + k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect DirectW ports
        if (directWMux.contains(i)) {
          val directWMuxPortMapping = directWMux(i)
          val directWMuxBufferBase = directWMux.accessParsBelowBufferPort(i).length // Index into NBuf io
          val sramDirectWPorts = directWMuxPortMapping.accessPars.length
          (0 until sramDirectWPorts).foreach {k => 
            f.io.directW(k).en := io.directW(directWMuxBufferBase + k).en
            f.io.directW(k).shiftEn := io.directW(directWMuxBufferBase + k).shiftEn
            f.io.directW(k).data := io.directW(directWMuxBufferBase + k).data
            f.io.directW(k).ofs := io.directW(directWMuxBufferBase + k).ofs
          }
        }

        // Connect BroadcastW ports
        if (hasBroadcastW) {
          val sramXBarWBase = if (xBarWMux.contains(i)) xBarWMux(i).values.size else 0
          val sramBroadcastWPorts = broadcastWMux.accessPars.length
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcastW(k).en
            f.io.xBarW(sramXBarWBase + k).shiftEn := io.broadcastW(k).shiftEn
            f.io.xBarW(sramXBarWBase + k).data := io.broadcastW(k).data
            f.io.xBarW(sramXBarWBase + k).ofs := io.broadcastW(k).ofs
            f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcastW(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect BroadcastR ports
        if (hasBroadcastR) {
          val sramXBarRBase = if (xBarRMux.contains(i)) xBarRMux(i).values.size else 0
          val sramBroadcastRPorts = broadcastRMux.accessPars.length
          (0 until sramBroadcastRPorts).foreach {k => 
            f.io.xBarR(sramXBarRBase + k).en := io.broadcastR(k).en
            // f.io.xBarR(sramXBarRBase + k).shiftEn := io.broadcastR(k).shiftEn
            // f.io.xBarR(sramXBarRBase + k).data := io.broadcastR(k).data
            f.io.xBarR(sramXBarRBase + k).ofs := io.broadcastR(k).ofs
            f.io.xBarR(sramXBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        if (xBarRMux.contains(i)) {
          val xBarRMuxPortMapping = xBarRMux(i)
          val xBarRMuxBufferBase = xBarRMux.accessParsBelowBufferPort(i).length // Index into NBuf io
          val outputXBarRMuxBufferBase = xBarRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val sramXBarRPorts = xBarRMuxPortMapping.accessPars.length
          (0 until sramXBarRPorts).foreach {k => 
            val port_width = xBarRMuxPortMapping.accessPars(k)
            val k_base = xBarRMuxPortMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(outputXBarRMuxBufferBase + (k_base + m)) := f.io.output.data(sram_index)
            }
            f.io.xBarR(k).en := io.xBarR(xBarRMuxBufferBase + k).en
            // f.io.xBarR(xBarRMuxBufferBase + k).data := io.xBarR(xBarRMuxBufferBase + k).data
            f.io.xBarR(k).ofs := io.xBarR(xBarRMuxBufferBase + k).ofs
            f.io.xBarR(k).banks.zip(io.xBarR(xBarRMuxBufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
              // f.io.flow(k) := io.flow(k) // Dangerous move here
          }
        }

        // Connect DirectR ports and the associated outputs
        if (directRMux.contains(i)) {
          val directRMuxPortMapping = directRMux(i)
          val directRMuxBufferBase = directRMux.accessParsBelowBufferPort(i).length // Index into NBuf io
          val xBarRBase = xBarRMux.accessPars.length
          val outputDirectRMuxBufferBase = directRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val outputXBarRBase = xBarRMux.accessPars.sum
          val sramDirectRPorts = directRMuxPortMapping.accessPars.length
          (0 until sramDirectRPorts).foreach {k => 
            val port_width = directRMuxPortMapping.accessPars(k)
            val k_base = directRMuxPortMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(outputXBarRBase + outputDirectRMuxBufferBase + (k_base + m)) := f.io.output.data(outputXBarRBase + k_base + m)
            }
            f.io.directR(k).en := io.directR(directRMuxBufferBase + k).en
            // f.io.directR(directRMuxBufferBase + k).data := io.directR(directRMuxBufferBase + k).data
            f.io.directR(k).ofs := io.directR(directRMuxBufferBase + k).ofs
            // f.io.flow(k + {if (hasXBarR) numXBarR else 0}) := io.flow(k + {if (hasXBarR) numXBarR else 0}) // Dangerous move here
          }
        }

        // Connect BroadcastR ports and the associated outputs
        // TODO: Broadcasting sram reads are untested and expected behavior is unclear, currently getting last buffer all the time just because
        val xBarRMuxPortMapping = broadcastRMux
        val xBarRMuxBufferBase = xBarRMux.accessPars.sum
        val sramXBarRPorts = xBarRMuxPortMapping.accessPars.sum
        (0 until sramXBarRPorts).foreach {k => 
          io.output.data(xBarRMuxBufferBase + k) := f.io.output.data(k)
          f.io.xBarR(xBarRMuxBufferBase + k).en := io.broadcastR(k).en
          f.io.xBarR(xBarRMuxBufferBase + k).ofs := io.broadcastR(k).ofs
          f.io.xBarR(xBarRMuxBufferBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
        }
      }
    case LineBufferType => 
      val rowstride = strides(0)
      val numrows = logicalDims(0) + (numBufs-1)*rowstride
      val numcols = logicalDims(1)
      val combinedXBarWMux = xBarWMux.mergeXMaps
      val combinedXBarRMux = xBarRMux.mergeXMaps
      val flatDirectWMux = directWMux.mergeDMaps 
      val flatDirectRMux = directRMux.mergeDMaps 
      val lb = Module(new BankedSRAM(List(numrows,numcols), bitWidth,
                               List(numrows,banks(1)), strides,
                               combinedXBarWMux, combinedXBarRMux,
                               flatDirectWMux, flatDirectRMux,
                               bankingMode, inits, syncMem, fracBits, "lb"))

      
      val numWriters = numXBarW + numDirectW
      val par = combinedXBarWMux.accessPars.sorted.headOption.getOrElse(0) max flatDirectWMux.accessPars.sorted.headOption.getOrElse(0)
      val writeCol = Module(new SingleCounter(par, Some(0), Some(numcols), Some(1), Some(0), false))
      val en = io.xBarW.map(_.en.reduce{_||_}).reduce{_||_} || io.directW.map(_.en.reduce{_||_}).reduce{_||_}
      writeCol.io.input.enable := en
      writeCol.io.input.reset := ctrl.io.swap
      writeCol.io.input.saturate := false.B

      val rowChanged = io.xBarW.map{p => getRetimed(p.banks(0),1) =/= p.banks(0)}.reduce{_||_}
      val gotFirstInRow = Module(new SRFF())
      gotFirstInRow.io.input.set := risingEdge(en) || (en && rowChanged)
      gotFirstInRow.io.input.reset :=  rowChanged | ctrl.io.swap
      gotFirstInRow.io.input.asyn_reset := false.B

      val base = chisel3.util.PriorityMux(io.xBarW.map(_.en).flatten, writeCol.io.output.count)
      val colCorrection = Module(new FF(32))
      colCorrection.io.xBarW(0).data.head := base.asUInt
      colCorrection.io.xBarW(0).init.head := 0.U
      colCorrection.io.xBarW(0).en.head := en & ~gotFirstInRow.io.output.data
      colCorrection.io.xBarW(0).reset.head := reset.toBool | risingEdge(!gotFirstInRow.io.output.data)
      val colCorrectionValue = Mux(en & ~gotFirstInRow.io.output.data, base.asUInt, colCorrection.io.output.data(0))

      val wCRN_width = 1 + log2Up(numrows)
      val writeRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), 0, wCRN_width))
      writeRow.io.input.enable := ctrl.io.swap
      writeRow.io.input.countUp := false.B

      // Connect XBarW ports
      xBarWMux.foreach { case (bufferPort, portMapping) =>
        val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
        val sramXBarWPorts = portMapping.accessPars.length
        // val wMask = getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
        (0 until sramXBarWPorts).foreach {k => 
          lb.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en
          lb.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
          lb.io.xBarW(bufferBase + k).ofs := writeCol.io.output.count.map{x => (x.asUInt - colCorrectionValue) / banks(1).U}
          lb.io.xBarW(bufferBase + k).banks.zipWithIndex.map{case (b,i) => 
            if (i % 2 == 0) b := writeRow.io.output.count + (rowstride-1).U - io.xBarW(bufferBase + k).banks(0)
            else b := (writeCol.io.output.count(i/2).asUInt - colCorrectionValue) % banks(1).U
          }
        }
      }

      val readRow = Module(new NBufCtr(rowstride, Some(0), Some(numrows), (numBufs-1)*rowstride, wCRN_width))
      readRow.io.input.enable := ctrl.io.swap
      readRow.io.input.countUp := false.B

      // Connect XBarR ports and the associated outputs
      xBarRMux.foreach { case (bufferPort, portMapping) =>
        val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
        val outputBufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
        val sramXBarRPorts = portMapping.accessPars.length
        // val rMask = getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
        // val outSel = (0 until numBufs).map{ a => getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (retime) 1 else 0}) }
        (0 until sramXBarRPorts).foreach {k => 
          val port_width = portMapping.sortByMuxPortAndOfs.accessPars(k)
          val k_base = portMapping.sortByMuxPortAndOfs.accessPars.take(k).sum
          (0 until port_width).foreach{m => 
            val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
            io.output.data(outputBufferBase + (k_base + m)) := lb.io.output.data(sram_index)
          }
          lb.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en
          lb.io.xBarR(bufferBase + k).flow := io.xBarR(bufferBase + k).flow
          // lb.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
          lb.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
          lb.io.xBarR(bufferBase + k).banks.odd.zip(io.xBarR(bufferBase+k).banks.odd).foreach{case (a:UInt,b:UInt) => a := b}
          lb.io.xBarR(bufferBase + k).banks.even.zip(io.xBarR(bufferBase+k).banks.even).foreach{case (a:UInt,b:UInt) => a := (b + readRow.io.output.count) % numrows.U}
        }
      }



    case LIFOType => 
      val fifo = Module(new LIFO(List(logicalDims.head), bitWidth, 
                                  banks, combinedXBarWMux, combinedXBarRMux))

      fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-numXBarW)}
      fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-numXBarR)}
      combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
        val base = combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2, muxAddr._3).sum
        (0 until entry._1).foreach{i => io.output.data(base + i) := fifo.io.output.data(i)}
      }
      io.full := fifo.io.asInstanceOf[FIFOInterface].full
      io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
      io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
      io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
      io.numel := fifo.io.asInstanceOf[FIFOInterface].numel
  }


  var usedMuxPorts = List[(String,(Int,Int,Int,Int,Int))]() // Check if the bufferPort, muxPort, muxAddr, lane, castgrp is taken for this connection style (xBar or direct)
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(hasXBarW)
    assert(!usedMuxPorts.contains(("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("XBarW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
    val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).length
    val muxBase = xBarWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
    io.xBarW(bufferBase + muxBase) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, flow: Bool): Seq[UInt] = {
    assert(hasXBarR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).length
      val muxBase = xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val outputBufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum
      val outputMuxBase = xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
        } else if (!ignoreCastInfo) {
          assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
        }
        io.xBarR(bufferBase + muxBase).connectLane(vecId,i,rBundle, flow)
      }
      io.output.data(outputBufferBase + outputMuxBase + vecId)
    }
  }

  def connectBroadcastWPort(wBundle: W_XBar, muxAddr: (Int, Int)): Unit = {
    val muxBase = broadcastWMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2,0).length
    io.broadcastW(muxBase) := wBundle
  }

  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectBroadcastRPort(rBundle, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, flow: Bool): Seq[UInt] = {
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val muxBase = broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val xBarRBase = xBarRMux.accessPars.length
      val directRBase = directRMux.accessPars.length
      val outputXBarRBase = xBarRMux.accessPars.sum
      val outputDirectRBase = directRMux.accessPars.sum
      val outputMuxBase = broadcastRMux.accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        io.broadcastR(muxBase).connectLane(vecId,i,rBundle, flow)
      }
      io.output.data(outputXBarRBase + outputDirectRBase + outputMuxBase + vecId)
    }
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(hasDirectW)
    assert(!usedMuxPorts.contains(("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to directW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("directW", (bufferPort,muxAddr._1,muxAddr._2,0,0))
    val bufferBase = directWMux.accessParsBelowBufferPort(bufferPort).length 
    val muxBase = directWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2, 0).length
    io.directW(bufferBase + muxBase) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, flow: Bool): Seq[UInt] = {
    assert(hasDirectR)
    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
      val castgrp = if (ignoreCastInfo) 0 else cg
      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
      val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).length
      val xBarRBase = xBarRMux.accessPars.length
      val muxBase = directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).length
      val outputBufferBase = directRMux.accessParsBelowBufferPort(bufferPort).sum
      val outputXBarRBase = xBarRMux.accessPars.sum
      val outputMuxBase = directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, effectiveOfs,castgrp).sum
      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
      if (bid == 0) {
        if (ignoreCastInfo && i == 0) {
          assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,0))
        } else if (!ignoreCastInfo) {
          assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
          usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,effectiveOfs,i,castgrp))
        }
        io.directR(bufferBase + muxBase).connectLane(vecId,i,rBundle, flow)
      }
      io.output.data(outputXBarRBase + outputBufferBase + outputMuxBase + vecId)
    }
  }

  def connectStageCtrl(done: Bool, en: Bool, port: Int): Unit = {
    io.sEn(port) := en
    io.sDone(port) := done
  }

}



class RegChainPass(val numBufs: Int, val bitWidth: Int, myName: String = "") extends Module { 

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val xBarW = HVec(Array.tabulate(1){i => Input(new W_XBar(1, 1, List(1), bitWidth))})
    val xBarR = HVec(Array.tabulate(numBufs){i => Input(new R_XBar(1, 1, List(1)))})
    val directW = HVec(Array.tabulate(1){i => Input(new W_Direct(1, 1, List(List(1)), bitWidth))})
    val directR = HVec(Array.tabulate(1){i => Input(new R_Direct(1, 1, List(List(1))))})
    val broadcastW = HVec(Array.tabulate(1){i => Input(new W_XBar(1, 1, List(1), bitWidth))})
    val broadcastR = HVec(Array.tabulate(1){i => Input(new R_XBar(1, 1, List(1)))})
    val reset = Input(Bool())

    // FIFO Specific
    val full = Output(Bool())
    val almostFull = Output(Bool())
    val empty = Output(Bool())
    val almostEmpty = Output(Bool())
    val numel = Output(UInt(32.W))    

    val output = new Bundle {
      val data  = Vec(numBufs, Output(UInt(bitWidth.W)))  
    }
  })

  override def desiredName = myName
  
  val wMap = NBufXMap(0 -> XMap((0,0,0) -> (1, None)))
  val rMap = NBufXMap((0 until numBufs).map{i => i -> XMap((0,0,0) -> (1, None)) }.toArray:_*)

  val nbufFF = Module(new NBufMem(FFType, List(1), numBufs, bitWidth, List(1), List(1), 
                                    wMap, rMap, NBufDMap(), NBufDMap(),
                                    XMap(), XMap(), BankedMemory
                                  ))
  io <> nbufFF.io

  def connectStageCtrl(done: Bool, en: Bool, port: Int): Unit = {
    io.sEn(port) := en
    io.sDone(port) := done
  }

  def chain_pass[T](dat: T, en: Bool): Unit = { // Method specifically for handling reg chains that pass counter values between metapipe stages
    dat match {
      case data: UInt       => io.xBarW(0).data.head := data
      case data: FixedPoint => io.xBarW(0).data.head := data.number
    }
    io.xBarW(0).en.head := en
    io.xBarW(0).reset.head := getRetimed(reset, 1)
    io.xBarW(0).init.head := 0.U
  }


  def read(i: Int): UInt = {
    io.output.data(i)
  }


}
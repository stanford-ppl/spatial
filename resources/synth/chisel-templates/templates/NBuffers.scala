package templates

import util._
import chisel3._
import chisel3.util._
import ops._
import fringe._
import chisel3.util.MuxLookup
import types._
import Utils._

import scala.collection.immutable.HashMap


/* Controller that is instantiated in NBuf templates to handle port -> module muxing */
class NBufController(numBufs: Int, portsWithWriter: List[Int]) extends Module {

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val statesInW = Vec(1 max portsWithWriter.distinct.length, Output(UInt((1+Utils.log2Up(numBufs)).W)))
    val statesInR = Vec(numBufs, Output(UInt((1+Utils.log2Up(numBufs)).W)))
    val swap = Output(Bool())
  })

  // Logic for recognizing state swapping
  val sEn_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val sDone_latch = (0 until numBufs).map{i => Module(new SRFF())}
  val swap = Wire(Bool())
  // Latch whether each buffer's stage is enabled and when they are done
  (0 until numBufs).foreach{ i => 
    sEn_latch(i).io.input.set := io.sEn(i) & ~io.sDone(i)
    sEn_latch(i).io.input.reset := Utils.getRetimed(swap,1)
    sEn_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
    sDone_latch(i).io.input.set := io.sDone(i)
    sDone_latch(i).io.input.reset := Utils.getRetimed(swap,1)
    sDone_latch(i).io.input.asyn_reset := Utils.getRetimed(reset, 1)
  }
  val anyEnabled = sEn_latch.map{ en => en.io.output.data }.reduce{_|_}
  swap := Utils.risingEdge(sEn_latch.zip(sDone_latch).zipWithIndex.map{ case ((en, done), i) => en.io.output.data === (done.io.output.data || io.sDone(i)) }.reduce{_&_} & anyEnabled)
  io.swap := swap

  // Counters for reporting writer and reader buffer pointers
  // Mapping input write ports to their appropriate bank
  val statesInW = portsWithWriter.distinct.sorted.zipWithIndex.map { case (t,i) =>
    val c = Module(new NBufCtr(1,Some(t), Some(numBufs), 1+Utils.log2Up(numBufs)))
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInW(i) := c.io.output.count
    (t -> c)
  }

  // Mapping input read ports to their appropriate bank
  val statesInR = (0 until numBufs).map{  i => 
    val c = Module(new NBufCtr(1,Some(i), Some(numBufs), 1+Utils.log2Up(numBufs)))
    c.io.input.enable := swap
    c.io.input.countUp := false.B
    io.statesInR(i) := c.io.output.count
    c
  }

  def lookup(id: Int): Int = { portsWithWriter.sorted.distinct.indexOf(id) }

}


class NBufMem(val mem: MemType, 
           val logicalDims: List[Int], val numBufs: Int, val bitWidth: Int, 
           val banks: List[Int], val strides: List[Int], 
           val xBarWMux: NBufXMap, val xBarRMux: NBufXMap, // bufferPort -> (muxPort -> accessPar)
           val directWMux: NBufDMap, val directRMux: NBufDMap,  // bufferPort -> (muxPort -> List(banks, banks, ...))
           val broadcastWMux: XMap, val broadcastRMux: XMap,  // Assume broadcasts are XBar
           val bankingMode: BankingMode, val inits: Option[List[Double]] = None, val syncMem: Boolean = false, val fracBits: Int = 0) extends Module { 

  // Overloaded constructers
  // Tuple unpacker
  def this(tuple: (MemType, List[Int], Int, Int, List[Int], List[Int], NBufXMap, NBufXMap, 
    NBufDMap, NBufDMap, XMap, XMap, BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7,tuple._8,tuple._9,tuple._10, tuple._11, tuple._12, tuple._13, None, false, 0)

  val depth = logicalDims.product // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = Utils.log2Up(depth/banks.product)
  val banksWidths = banks.map(Utils.log2Up(_))

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
          directWMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).flatten.toList(i) 
        } else defaultDirect
        Input(new W_Direct(directWMux.accessPars.getOr1(i), ofsWidth, dBanks, bitWidth))
      })
    val directR = HVec(Array.tabulate(1 max numDirectRPorts){i => 
        val dBanks = if (hasDirectR) {
          directRMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).flatten.toList(i) 
        } else defaultDirect
        Input(new R_Direct(directRMux.accessPars.getOr1(i), ofsWidth, dBanks))
      })
    val broadcastW = HVec(Array.tabulate(1 max numBroadcastWPorts){i => Input(new W_XBar(broadcastWMux.accessPars.getOr1(i), ofsWidth, banksWidths, bitWidth))})
    val broadcastR = HVec(Array.tabulate(1 max numBroadcastRPorts){i => Input(new R_XBar(broadcastRMux.accessPars.getOr1(i), ofsWidth, banksWidths))})
    val flow = Vec(1 max totalOutputs, Input(Bool()))

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
    case SRAMType => 
      val srams = (0 until numBufs).map{ i => 
        Module(new SRAM(logicalDims, bitWidth, 
                        banks, strides, 
                        combinedXBarWMux, combinedXBarRMux,
                        flatDirectWMux, flatDirectRMux,
                        bankingMode, inits, syncMem, fracBits))
      }
      // Route NBuf IO to SRAM IOs
      srams.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.length
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
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
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
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
            f.io.xBarR(sramXBarRBase + k).data := io.broadcastR(k).data
            f.io.xBarR(sramXBarRBase + k).ofs := io.broadcastR(k).ofs
            f.io.xBarR(sramXBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        xBarRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val sramXBarRPorts = portMapping.accessPars.length
          val rMask = Utils.getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
          (0 until sramXBarRPorts).foreach {k => 
            val port_width = portMapping.accessPars(k)
            val k_base = portMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(bufferBase + (k_base + m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(sram_index)})
            }
            f.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en.map(_ & rMask)
            // f.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
            f.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
            f.io.xBarR(bufferBase + k).banks.zip(io.xBarR(bufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
            f.io.flow(k) := io.flow(k) // Dangerous move here
          }
        }

        // Connect DirectR ports and the associated outputs
        directRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).length // Index into NBuf io
          val xBarRBase = xBarRMux.accessPars.length
          val sramDirectRPorts = portMapping.accessPars.length
          val rMask = Utils.getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
          (0 until sramDirectRPorts).foreach {k => 
            val port_width = portMapping.accessPars(k)
            val k_base = portMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - portMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => portMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(xBarRBase + bufferBase + (k_base + m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(sram_index)})
            }

            f.io.directR(bufferBase + k).en := io.directR(k).en.map(_ & rMask)
            // f.io.directR(bufferBase + k).data := io.directR(bufferBase + k).data
            f.io.directR(bufferBase + k).ofs := io.directR(k).ofs
            f.io.flow(k + numXBarR) := io.flow(k + numXBarR) // Dangerous move here
          }
        }

        // Connect BroadcastR ports and the associated outputs
        // TODO: Broadcasting sram reads are untested and expected behavior is unclear, currently getting last buffer all the time just because
        val xBarRBase = xBarRMux.accessPars.length
        val directRBase = directRMux.accessPars.length
        val sramXBarRPorts = broadcastRMux.accessPars.length
        val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR.head === a.U, {if (Utils.retime) 1 else 0}) }
        (0 until sramXBarRPorts).foreach {k => 
          val port_width = broadcastRMux.accessPars(k)
          val k_base = broadcastRMux.accessPars.take(k).sum
          (0 until port_width).foreach{m => 
            io.output.data(xBarRBase + directRBase + (k_base+m)) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data((k_base+m))})
          }
         
          f.io.xBarR(xBarRBase + k).en := io.broadcastR( k).en
          // f.io.xBarR(xBarRBase + k).data := io.xBarR(xBarRBase + k).data
          f.io.xBarR(xBarRBase + k).ofs := io.broadcastR( k).ofs
          f.io.xBarR(xBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          f.io.flow(xBarRBase + k) := io.flow(xBarRBase + directRBase + k) // Dangerous move here
        }

      }
    case FFType => 
      val ffs = (0 until numBufs).map{ i => 
        Module(new FF(bitWidth, combinedXBarWMux, combinedXBarRMux, inits, fracBits)) 
      }
      // Route NBuf IO to FF IOs
      ffs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
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
        val sel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
        (0 until sramXBarRPorts).foreach {k => io.output.data(bufferBase+k) := chisel3.util.Mux1H(sel, ffs.map{f => f.io.output.data(0)})}
      }

      // TODO: BroadcastR connections?
      val xBarRBase = xBarRMux.accessPars.sum
      val sramBroadcastRPorts = broadcastRMux.accessPars.sum
      val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR.head === a.U, {if (Utils.retime) 1 else 0})}
      (0 until sramBroadcastRPorts).foreach {k => io.output.data(xBarRBase + k) := chisel3.util.Mux1H(outSel, ffs.map{f => f.io.output.data(0)}) }
      
    case FIFOType => 
      val fifo = Module(new FIFO(List(logicalDims.head), bitWidth, 
                                  banks, combinedXBarWMux, combinedXBarRMux))

      fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-numXBarW)}
      fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-numXBarR)}
      fifo.io.flow := io.flow
      combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
        val base = combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2).sum
        (0 until entry._1).foreach{i => io.output.data(base + i) := fifo.io.output.data(i)}
      }
      io.full := fifo.io.asInstanceOf[FIFOInterface].full
      io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
      io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
      io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
      io.numel := fifo.io.asInstanceOf[FIFOInterface].numel

    case ShiftRegFileType => 
      val rfs = (0 until numBufs).map{ i => 
        val combinedXBarWMux = xBarWMux.getOrElse(i,XMap()).merge(broadcastWMux)
        val combinedXBarRMux = xBarRMux.getOrElse(i,XMap()).merge(broadcastRMux)
        Module(new ShiftRegFile(logicalDims, bitWidth, 
                        combinedXBarWMux, combinedXBarRMux,
                        directWMux.getOrElse(i, DMap()), directRMux.getOrElse(i,DMap()),
                        inits, syncMem, fracBits, isBuf = {i != 0}))
      }
      rfs.drop(1).zipWithIndex.foreach{case (rf, i) => rf.io.asInstanceOf[ShiftRegFileInterface].dump_in.zip(rfs(i).io.asInstanceOf[ShiftRegFileInterface].dump_out).foreach{case(a,b) => a:=b}; rf.io.asInstanceOf[ShiftRegFileInterface].dump_en := ctrl.io.swap}

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
            f.io.xBarR(sramXBarRBase + k).data := io.broadcastR(k).data
            f.io.xBarR(sramXBarRBase + k).ofs := io.broadcastR(k).ofs
            f.io.xBarR(sramXBarRBase + k).banks.zip(io.broadcastR(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        if (xBarRMux.contains(i)) {
          val xBarRMuxPortMapping = xBarRMux(i)
          val xBarRMuxBufferBase = xBarRMux.accessParsBelowBufferPort(i).length // Index into NBuf io
          val sramXBarRPorts = xBarRMuxPortMapping.accessPars.length
          (0 until sramXBarRPorts).foreach {k => 
            val port_width = xBarRMuxPortMapping.accessPars(k)
            val k_base = xBarRMuxPortMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => xBarRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(xBarRMuxBufferBase + (k_base + m)) := f.io.output.data(k_base + m)
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
          val sramDirectRPorts = directRMuxPortMapping.accessPars.length
          (0 until sramDirectRPorts).foreach {k => 
            val port_width = directRMuxPortMapping.accessPars(k)
            val k_base = directRMuxPortMapping.accessPars.take(k).sum
            (0 until port_width).foreach{m => 
              val sram_index = (k_base + m) - directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.indices.map{i => directRMuxPortMapping.sortByMuxPortAndCombine.accessPars.take(i+1).sum}.filter((k_base + m) >= _).lastOption.getOrElse(0)
              io.output.data(xBarRBase + directRMuxBufferBase + (k_base + m)) := f.io.output.data(xBarRBase + k_base + m)
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
    case LIFOType => 
      val fifo = Module(new LIFO(List(logicalDims.head), bitWidth, 
                                  banks, combinedXBarWMux, combinedXBarRMux))

      fifo.io.xBarW.zipWithIndex.foreach{case (f, i) => if (i < numXBarW) f := io.xBarW(i) else f := io.broadcastW(i-numXBarW)}
      fifo.io.xBarR.zipWithIndex.foreach{case (f, i) => if (i < numXBarR) f := io.xBarR(i) else f := io.broadcastR(i-numXBarR)}
      fifo.io.flow := io.flow
      combinedXBarRMux.sortByMuxPortAndOfs.foreach{case (muxAddr, entry) => 
        val base = combinedXBarRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2).sum
        (0 until entry._1).foreach{i => io.output.data(base + i) := fifo.io.output.data(i)}
      }
      io.full := fifo.io.asInstanceOf[FIFOInterface].full
      io.almostFull := fifo.io.asInstanceOf[FIFOInterface].almostFull
      io.empty := fifo.io.asInstanceOf[FIFOInterface].empty
      io.almostEmpty := fifo.io.asInstanceOf[FIFOInterface].almostEmpty
      io.numel := fifo.io.asInstanceOf[FIFOInterface].numel
  }


  var usedMuxPorts = List[(String,(Int,Int,Int))]() // Check if the bufferPort, muxPort, muxAddr is taken for this connection style (xBar or direct)
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(hasXBarW)
    assert(!usedMuxPorts.contains(("XBarW", (bufferPort,muxAddr._1,muxAddr._2))), s"Attempted to connect to XBarW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("XBarW", (bufferPort,muxAddr._1,muxAddr._2))
    val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).length
    val muxBase = xBarWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    io.xBarW(bufferBase + muxBase) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int)): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), flow: Bool): Seq[UInt] = {
    assert(hasXBarR)
    assert(!usedMuxPorts.contains(("XBarR", (bufferPort,muxAddr._1,muxAddr._2))), s"Attempted to connect to XBarR port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("XBarR", (bufferPort,muxAddr._1,muxAddr._2))
    val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).length
    val muxBase = xBarRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    io.xBarR(bufferBase + muxBase) := rBundle    
    io.flow(bufferBase + muxBase) := flow
    rBundle.port_width.indices[UInt]{vecId => io.output.data(bufferBase + muxBase + vecId)}
  }

  def connectBroadcastWPort(wBundle: W_XBar, muxAddr: (Int, Int)): Unit = {
    val muxBase = broadcastWMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    io.broadcastW(muxBase) := wBundle
  }

  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int)): Seq[UInt] = {connectBroadcastRPort(rBundle, muxAddr, true.B)}
  def connectBroadcastRPort(rBundle: R_XBar, muxAddr: (Int, Int), flow: Bool): Seq[UInt] = {
    val muxBase = broadcastRMux.accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    val xBarRBase = xBarRMux.accessPars.length
    val directRBase = directRMux.accessPars.length
    io.broadcastR(muxBase) := rBundle
    io.flow(xBarRBase + directRBase + muxBase) := flow
    rBundle.port_width.indices[UInt]{vecId => io.output.data(xBarRBase + directRBase + muxBase + vecId)}
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
    assert(hasDirectW)
    assert(!usedMuxPorts.contains(("directW", (bufferPort,muxAddr._1,muxAddr._2))), s"Attempted to connect to directW port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("directW", (bufferPort,muxAddr._1,muxAddr._2))
    val bufferBase = directWMux.accessParsBelowBufferPort(bufferPort).length 
    val muxBase = directWMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    io.directW(bufferBase + muxBase) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int)): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), flow: Bool): Seq[UInt] = {
    assert(hasDirectR)
    assert(!usedMuxPorts.contains(("directR", (bufferPort,muxAddr._1,muxAddr._2))), s"Attempted to connect to directR port ($bufferPort,$muxAddr) twice!")
    usedMuxPorts ::= ("directR", (bufferPort,muxAddr._1,muxAddr._2))
    val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).length
    val xBarRBase = xBarRMux.accessPars.length
    val muxBase = directRMux(bufferPort).accessParsBelowMuxPort(muxAddr._1, muxAddr._2).length
    io.directR(bufferBase + muxBase) := rBundle    
    io.flow(xBarRBase + bufferBase + muxBase) := flow
    rBundle.port_width.indices[UInt]{vecId => io.output.data(xBarRBase + bufferBase + muxBase + vecId)}
  }

  def connectStageCtrl(done: Bool, en: Bool, port: Int): Unit = {
    io.sEn(port) := en
    io.sDone(port) := done
  }
 
  // def connectUntouchedPorts(ports: List[Int]) {
  //   ports.foreach{ port => 
  //     io.sEn(port) := false.B
  //     io.sDone(port) := false.B
  //   }
  // }

  // def connectDummyBroadcast() {
  //   (0 until bPar.reduce{_+_}).foreach { i =>
  //     io.broadcast(i).en := false.B
  //   }
  // }

}



class RegChainPass(val numBufs: Int, val bitWidth: Int) extends Module { 

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val xBarW = HVec(Array.tabulate(1){i => Input(new W_XBar(1, 1, List(1), bitWidth))})
    val xBarR = HVec(Array.tabulate(numBufs){i => Input(new R_XBar(1, 1, List(1)))})
    val directW = HVec(Array.tabulate(1){i => Input(new W_Direct(1, 1, List(List(1)), bitWidth))})
    val directR = HVec(Array.tabulate(1){i => Input(new R_Direct(1, 1, List(List(1))))})
    val broadcastW = HVec(Array.tabulate(1){i => Input(new W_XBar(1, 1, List(1), bitWidth))})
    val broadcastR = HVec(Array.tabulate(1){i => Input(new R_XBar(1, 1, List(1)))})
    val flow = Vec(numBufs, Input(Bool()))

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

  val wMap = NBufXMap(0 -> XMap((0,0) -> (1, None)))
  val rMap = NBufXMap((0 until numBufs).map{i => 
    (i -> XMap((0,0) -> (1, None)))
  }.toArray:_*)

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
      case data: UInt => 
        io.xBarW(0).data.head := data
      case data: FixedPoint => 
        io.xBarW(0).data.head := data.number
    }
    io.xBarW(0).en.head := en
    io.xBarW(0).reset.head := Utils.getRetimed(reset, 1)
    io.xBarW(0).init.head := 0.U
  }


  def read(i: Int): UInt = {
    io.output.data(i)
  }


}
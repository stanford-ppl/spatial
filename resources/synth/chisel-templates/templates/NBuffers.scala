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
           val broadcastWMux: XMap, // Assume broadcasts are XBar
           val bankingMode: BankingMode, val inits: Option[List[Double]] = None, val syncMem: Boolean = false, val fracBits: Int = 0) extends Module { 

  // Overloaded constructers
  // Tuple unpacker
  def this(tuple: (MemType, List[Int], Int, Int, List[Int], List[Int], NBufXMap, NBufXMap, 
    NBufDMap, NBufDMap, XMap, BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7,tuple._8,tuple._9,tuple._10, tuple._11, tuple._12, None, false, 0)

  val depth = logicalDims.product // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = Utils.log2Up(depth/banks.product)
  val banksWidths = banks.map(Utils.log2Up(_))

  // Compute info required to set up IO interface
  val hasXBarW = xBarWMux.accessPars.sum > 0
  val hasXBarR = xBarRMux.accessPars.sum > 0
  val numXBarW = if (hasXBarW) xBarWMux.accessPars.sum else 0
  val numXBarR = if (hasXBarR) xBarRMux.accessPars.sum else 0
  val hasDirectW = directWMux.mergeDMaps.accessPars.sum > 0
  val hasDirectR = directRMux.mergeDMaps.accessPars.sum > 0
  val numDirectW = if (hasDirectW) directWMux.mergeDMaps.accessPars.sum else 0
  val numDirectR = if (hasDirectR) directRMux.mergeDMaps.accessPars.sum else 0
  val totalOutputs = numXBarR + numDirectR
  val hasBroadcastW = broadcastWMux.accessPars.toList.sum > 0
  val numBroadcastW = if (hasBroadcastW) broadcastWMux.accessPars.toList.sum else 0
  val defaultDirect = List.fill(banks.length)(99)
  val portsWithWriter = (directWMux.keys ++ xBarWMux.keys).toList.sorted

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val xBarW = Vec(1 max numXBarW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val xBarR = Vec(1 max numXBarR, Input(new R_XBar(ofsWidth, banksWidths))) 
    val directW = HVec(Array.tabulate(1 max numDirectW){i => Input(new W_Direct(ofsWidth, if (hasDirectW) directWMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).flatten.flatten.toList(i) else defaultDirect, bitWidth))})
    val directR = HVec(Array.tabulate(1 max numDirectR){i => Input(new R_Direct(ofsWidth, if (hasDirectR) directRMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values.map(_._1)).flatten.flatten.toList(i) else defaultDirect))})
    val broadcast = Vec(1 max numBroadcastW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val flow = Vec(numXBarR + numDirectR, Input(Bool()))

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

  // Create physical mems
  mem match {
    case SRAMType => 
      val srams = (0 until numBufs).map{ i => 
        Module(new SRAM(logicalDims, bitWidth, 
                        banks, strides, 
                        combinedXBarWMux, flatXBarRMux,
                        flatDirectWMux, flatDirectRMux,
                        bankingMode, inits, syncMem, fracBits))
      }
      // Route NBuf IO to SRAM IOs
      srams.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en & wMask
            f.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
            f.io.xBarW(bufferBase + k).ofs := io.xBarW(bufferBase + k).ofs
            f.io.xBarW(bufferBase + k).banks.zip(io.xBarW(bufferBase + k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect DirectW ports
        directWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = directWMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramDirectWPorts = portMapping.accessPars.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramDirectWPorts).foreach {k => 
            f.io.directW(bufferBase + k).en := io.directW(bufferBase + k).en & wMask
            f.io.directW(bufferBase + k).data := io.directW(bufferBase + k).data
            f.io.directW(bufferBase + k).ofs := io.directW(bufferBase + k).ofs
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.accessPars.sum
          val sramBroadcastWPorts = broadcastWMux.accessPars.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcast(k).en
            f.io.xBarW(sramXBarWBase + k).data := io.broadcast(k).data
            f.io.xBarW(sramXBarWBase + k).ofs := io.broadcast(k).ofs
            f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcast(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        xBarRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarRPorts = portMapping.accessPars.sum
          val rMask = Utils.getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
          (0 until sramXBarRPorts).foreach {k => 
            io.output.data(bufferBase + k) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(k)})
            f.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en & rMask
            // f.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
            f.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
            f.io.xBarR(bufferBase + k).banks.zip(io.xBarR(bufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
            f.io.flow(k) := io.flow(k) // Dangerous move here
          }
        }

        // Connect DirectR ports and the associated outputs
        directRMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val xBarRBase = xBarRMux.accessPars.sum
          val sramDirectRPorts = portMapping.accessPars.sum
          val rMask = Utils.getRetimed(ctrl.io.statesInR(bufferPort) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          val outSel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
          (0 until sramDirectRPorts).foreach {k => 
            io.output.data(xBarRBase + bufferBase + k) := chisel3.util.Mux1H(outSel, srams.map{f => f.io.output.data(k)})
            f.io.directR(bufferBase + k).en := io.directR(k).en & rMask
            // f.io.directR(bufferBase + k).data := io.directR(bufferBase + k).data
            f.io.directR(bufferBase + k).ofs := io.directR(k).ofs
            f.io.flow(k + {if (hasXBarR) numXBarR else 0}) := io.flow(k + {if (hasXBarR) numXBarR else 0}) // Dangerous move here
          }
        }
      }
    case FFType => 
      val ffs = (0 until numBufs).map{ i => 
        Module(new FF(bitWidth, combinedXBarWMux, inits, fracBits))
      }
      // Route NBuf IO to FF IOs
      ffs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (bufferPort, portMapping) =>
          val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
          val sramXBarWPorts = portMapping.accessPars.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(ctrl.lookup(bufferPort)) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this bufferPort to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en & wMask
            f.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.accessPars.sum
          val sramBroadcastWPorts = broadcastWMux.accessPars.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcast(k).en
            f.io.xBarW(sramXBarWBase + k).data := io.broadcast(k).data
          }
        }
      }

      // Connect buffers to output data ports
      xBarRMux.foreach { case (bufferPort, portMapping) => 
        val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum // Index into NBuf io
        val sel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesInR(bufferPort) === a.U, {if (Utils.retime) 1 else 0}) }
        io.output.data(bufferBase) := chisel3.util.Mux1H(sel, ffs.map{f => f.io.output.data(0)})        
      }
    case FIFOType => 
      val fifo = Module(new FIFO(List(logicalDims.head*depth), bitWidth, 
                                  banks, combinedXBarWMux, flatXBarRMux))

      fifo.io.xBarW := io.xBarW
      fifo.io.xBarR := io.xBarR
      fifo.io.flow := io.flow
      io.output.data := fifo.io.output.data
      io.full := fifo.io.full
      io.almostFull := fifo.io.almostFull
      io.empty := fifo.io.empty
      io.almostEmpty := fifo.io.almostEmpty
      io.numel := fifo.io.numel

    case ShiftRegFileType => 
      val rfs = (0 until numBufs).map{ i => 
        val combinedXBarWMux = xBarWMux.getOrElse(i,XMap()).merge(broadcastWMux)
        Module(new ShiftRegFile(logicalDims, bitWidth, 
                        combinedXBarWMux, xBarRMux.getOrElse(i, XMap()),
                        directWMux.getOrElse(i, DMap()), directRMux.getOrElse(i,DMap()),
                        inits, syncMem, fracBits, isBuf = {i != 0}))
      }
      rfs.drop(1).zipWithIndex.foreach{case (rf, i) => rf.io.dump_in.zip(rfs(i).io.output.dump_out).foreach{case(a,b) => a:=b}; rf.io.dump_en := ctrl.io.swap}

      // Route NBuf IO to SRAM IOs
      rfs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        if (xBarWMux.contains(i)) {
          val xBarWMuxPortMapping = xBarWMux(i)
          val xBarWMuxBufferBase = xBarWMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val sramXBarWPorts = xBarWMuxPortMapping.accessPars.sum
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
          val directWMuxBufferBase = directWMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val sramDirectWPorts = directWMuxPortMapping.accessPars.sum
          (0 until sramDirectWPorts).foreach {k => 
            f.io.directW(k).en := io.directW(directWMuxBufferBase + k).en
            f.io.directW(k).shiftEn := io.directW(directWMuxBufferBase + k).shiftEn
            f.io.directW(k).data := io.directW(directWMuxBufferBase + k).data
            f.io.directW(k).ofs := io.directW(directWMuxBufferBase + k).ofs
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = if (xBarWMux.contains(i)) xBarWMux(i).values.map(_._1).sum else 0
          val sramBroadcastWPorts = broadcastWMux.accessPars.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcast(k).en
            f.io.xBarW(sramXBarWBase + k).shiftEn := io.broadcast(k).shiftEn
            f.io.xBarW(sramXBarWBase + k).data := io.broadcast(k).data
            f.io.xBarW(sramXBarWBase + k).ofs := io.broadcast(k).ofs
            f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcast(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports and the associated outputs
        if (xBarRMux.contains(i)) {
          val xBarRMuxPortMapping = xBarRMux(i)
          val xBarRMuxBufferBase = xBarRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val sramXBarRPorts = xBarRMuxPortMapping.accessPars.sum
          (0 until sramXBarRPorts).foreach {k => 
            io.output.data(xBarRMuxBufferBase + k) := f.io.output.data(k)
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
          val directRMuxBufferBase = directRMux.accessParsBelowBufferPort(i).sum // Index into NBuf io
          val xBarRBase = xBarRMux.accessPars.sum
          val sramDirectRPorts = directRMuxPortMapping.accessPars.sum
          (0 until sramDirectRPorts).foreach {k => 
            io.output.data(xBarRBase + directRMuxBufferBase + k) := f.io.output.data(xBarRBase + k)
            f.io.directR(k).en := io.directR(directRMuxBufferBase + k).en
            // f.io.directR(directRMuxBufferBase + k).data := io.directR(directRMuxBufferBase + k).data
            f.io.directR(k).ofs := io.directR(directRMuxBufferBase + k).ofs
            // f.io.flow(k + {if (hasXBarR) numXBarR else 0}) := io.flow(k + {if (hasXBarR) numXBarR else 0}) // Dangerous move here
          }
        }
      }

  }



  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxPort: Int) {connectXBarWPort(wBundle, bufferPort, muxPort, 0)}
  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxPort: Int, vecId: Int) {
    assert(hasXBarW)
    val bufferBase = xBarWMux.accessParsBelowBufferPort(bufferPort).sum
    val muxBase = xBarWMux(bufferPort).accessParsBelowMuxPort(muxPort).sum + vecId
    io.xBarW(bufferBase + muxBase) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxPort: Int, vecId: Int): UInt = {connectXBarRPort(rBundle, bufferPort, muxPort, vecId, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxPort: Int): UInt = {connectXBarRPort(rBundle, bufferPort, muxPort, 0, true.B)}
  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    assert(hasXBarR)
    val bufferBase = xBarRMux.accessParsBelowBufferPort(bufferPort).sum
    val muxBase = xBarRMux(bufferPort).accessParsBelowMuxPort(muxPort).sum + vecId
    io.xBarR(bufferBase + muxBase) := rBundle    
    io.flow(bufferBase + muxBase) := flow
    io.output.data(bufferBase + muxBase)
  }

  def connectBroadcastPort(wBundle: W_XBar, muxPort: Int) {connectBroadcastPort(wBundle, muxPort, 0)}
  def connectBroadcastPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val muxBase = broadcastWMux.accessParsBelowMuxPort(muxPort).sum + vecId
    io.broadcast(muxBase) := wBundle
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxPort: Int, vecId: Int) {
    assert(hasDirectW)
    val bufferBase = directWMux.accessParsBelowBufferPort(bufferPort).sum 
    val muxBase = directWMux(bufferPort).accessParsBelowMuxPort(muxPort).sum + vecId
    io.directW(bufferBase + muxBase) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxPort: Int, vecId: Int): UInt = {connectDirectRPort(rBundle, bufferPort, muxPort, vecId, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    assert(hasDirectR)
    val bufferBase = directRMux.accessParsBelowBufferPort(bufferPort).sum
    val xBarRBase = xBarRMux.accessPars.sum
    val muxBase = directRMux(bufferPort).accessParsBelowMuxPort(muxPort).sum + vecId
    io.directR(bufferBase + muxBase) := rBundle    
    io.flow(xBarRBase + bufferBase + muxBase) := flow
    io.output.data(xBarRBase + bufferBase + muxBase)
  }

  def connectBroadcastWPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val muxBase = broadcastWMux.accessParsBelowMuxPort(muxPort).sum + vecId
    io.broadcast(muxBase) := wBundle
  }

  def connectStageCtrl(done: Bool, en: Bool, port: Int) {
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
    val xBarW = Vec(1, Input(new W_XBar(1, List(1), bitWidth)))
    val xBarR = Vec(numBufs, Input(new R_XBar(1, List(1)))) 
    val directW = HVec(Array.tabulate(1){i => Input(new W_Direct(1, List(1), bitWidth))})
    val directR = HVec(Array.tabulate(1){i => Input(new R_Direct(1, List(1)))})
    val broadcast = Vec(1, Input(new W_XBar(1, List(1), bitWidth)))
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

  val wMap = NBufXMap(0 -> XMap(0 -> 1))
  val rMap = NBufXMap((0 until numBufs).map{i => 
    (i -> XMap(0 -> 1))
  }.toArray:_*)

  val nbufFF = Module(new NBufMem(FFType, List(1), numBufs, bitWidth, List(1), List(1), 
                                    wMap, rMap, NBufDMap(), NBufDMap(),
                                    XMap(), BankedMemory
                                  ))
  io <> nbufFF.io

  def connectStageCtrl(done: Bool, en: Bool, port: Int) {
    io.sEn(port) := en
    io.sDone(port) := done
  }

  def chain_pass[T](dat: T, en: Bool) { // Method specifically for handling reg chains that pass counter values between metapipe stages
    dat match {
      case data: UInt => 
        io.xBarW(0).data := data
      case data: FixedPoint => 
        io.xBarW(0).data := data.number
    }
    io.xBarW(0).en := en
    io.xBarW(0).reset := Utils.getRetimed(reset, 1)
    io.xBarW(0).init := 0.U
  }


  def read(i: Int): UInt = {
    io.output.data(i)
  }


}
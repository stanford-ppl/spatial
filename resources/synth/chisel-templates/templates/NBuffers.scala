package templates

import util._
import chisel3._
import chisel3.util._
import ops._
import fringe._
import chisel3.util.MuxLookup

import scala.collection.mutable.HashMap

class NBufMem(val mem: MemPrimitive, 
           val logicalDims: List[Int], val numBufs: Int, val bitWidth: Int, 
           val banks: List[Int], val strides: List[Int], 
           val xBarWMux: HashMap[Int, HashMap[Int, Int]], val xBarRMux: HashMap[Int, HashMap[Int, Int]], // buffer -> (muxPort -> accessPar)
           val directWMux: HashMap[Int, HashMap[Int, List[List[Int]]]], val directRMux: HashMap[Int, HashMap[Int, List[List[Int]]]],  // buffer -> (muxPort -> List(banks, banks, ...))
           val broadcastWMux: HashMap[Int, Int], // Assume broadcasts are XBar
           val bankingMode: BankingMode, val syncMem: Boolean = false) extends Module { 

  // Overloaded constructers
  // Tuple unpacker
  def this(tuple: (MemPrimitive, List[Int], Int, Int, List[Int], List[Int], HashMap[Int, HashMap[Int, Int]], HashMap[Int, HashMap[Int, Int]], 
    HashMap[Int, HashMap[Int,List[List[Int]]]], HashMap[Int, HashMap[Int, List[List[Int]]]], HashMap[Int, Int], BankingMode)) = this(tuple._1,tuple._2,tuple._3,tuple._4,tuple._5,tuple._6,tuple._7,tuple._8,tuple._9,tuple._10, tuple._11, tuple._12, false)

  val depth = logicalDims.product // Size of memory
  val N = logicalDims.length // Number of dimensions
  val ofsWidth = Utils.log2Up(depth/banks.product)
  val banksWidths = banks.map(Utils.log2Up(_))

  // Compute info required to set up IO interface
  val hasXBarW = xBarWMux.values.map(_.values).toList.flatten.sum > 0
  val hasXBarR = xBarRMux.values.map(_.values).toList.flatten.sum > 0
  val numXBarW = if (hasXBarW) xBarWMux.values.map(_.values).toList.flatten.sum else 1
  val numXBarR = if (hasXBarR) xBarRMux.values.map(_.values).toList.flatten.sum else 1
  val hasDirectW = directWMux.values.map(_.values).flatten.toList.flatten.length > 0
  val hasDirectR = directRMux.values.map(_.values).flatten.toList.flatten.length > 0
  val numDirectW = if (hasDirectW) directWMux.values.map(_.values).flatten.toList.flatten.length else 1
  val numDirectR = if (hasDirectR) directRMux.values.map(_.values).flatten.toList.flatten.length else 1
  val totalOutputs = {if (hasXBarR) xBarRMux.values.map(_.values.max).sum else 0} max {if (hasDirectR) directRMux.values.map(_.values.map(_.length).max).sum else 0}
  val hasBroadcastW = broadcastWMux.values.toList.sum > 0
  val numBroadcastW = if (hasBroadcastW) broadcastWMux.values.toList.sum else 1
  val defaultDirect = List.fill(banks.length)(99)
  val portsWithWriter = (directWMux.keys ++ xBarWMux.keys).toList.sorted

  val io = IO( new Bundle {
    val sEn = Vec(numBufs, Input(Bool()))
    val sDone = Vec(numBufs, Input(Bool()))
    val xBarW = Vec(numXBarW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val xBarR = Vec(numXBarR, Input(new R_XBar(ofsWidth, banksWidths))) 
    val directW = HVec(Array.tabulate(numDirectW){i => Input(new W_Direct(ofsWidth, if (hasDirectW) directRMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values).flatten.flatten.toList(i) else defaultDirect, bitWidth))})
    val directR = HVec(Array.tabulate(numDirectR){i => Input(new R_Direct(ofsWidth, if (hasDirectR) directRMux.toSeq.sortBy(_._1).toMap.values.map(_.toSeq.sortBy(_._1).toMap.values).flatten.flatten.toList(i) else defaultDirect))})
    val broadcast = Vec(numBroadcastW, Input(new W_XBar(ofsWidth, banksWidths, bitWidth)))
    val flow = Vec({if (hasXBarR) numXBarR else 0} + {if (hasDirectR) numDirectR else 0}, Input(Bool()))
    val output = new Bundle {
      val data  = Vec(numBufs*totalOutputs, Output(UInt(bitWidth.W)))  
    }
  })

  // Instantiate buffer controller
  val ctrl = Module(new NBufController(numBufs, portsWithWriter))
  for (i <- 0 until numBufs){
    ctrl.io.sEn(i) := io.sEn(i)
    ctrl.io.sDone(i) := io.sDone(i)
  }

  // Flatten buffer/mux port info and provide each one to each mem
  val flatXBarWMux = HashMap(xBarWMux.toList.sortBy(_._1).map{case (buf,map) => 
      val base = xBarWMux.filter(_._1 < buf).values.toList.flatten.map(_._1).length
      map.map{case (muxport, par) => ({muxport + base} -> par)} 
    }.flatten.toArray:_*) 
  val flatXBarRMux = HashMap(xBarRMux.toList.sortBy(_._1).map{case (buf,map) => 
      val base = xBarRMux.filter(_._1 < buf).values.toList.flatten.map(_._1).length
      map.map{case (muxport, par) => ({muxport + base} -> par)} 
    }.flatten.toArray:_*) 
  val flatDirectWMux = HashMap(directWMux.toList.sortBy(_._1).map{case (buf,map) => 
      val base = directWMux.filter(_._1 < buf).values.toList.flatten.map(_._1).length
      map.map{case (muxport, banks) => ({muxport + base} -> banks)} 
    }.flatten.toArray:_*) 
  val flatDirectRMux = HashMap(directRMux.toList.sortBy(_._1).map{case (buf,map) => 
      val base = directRMux.filter(_._1 < buf).values.toList.flatten.map(_._1).length
      map.map{case (muxport, banks) => ({muxport + base} -> banks)} 
    }.flatten.toArray:_*) 
  val combinedXBarWMux = if (hasBroadcastW) {
      HashMap( (flatXBarWMux ++ HashMap(broadcastWMux.map{case (k,v) => 
                                                            val base = flatXBarWMux.toList.length
                                                            ({base + k} -> v)
                                                          }.toArray:_*)).toArray:_*)
                                                  } else flatXBarWMux
  // Create physical mems
  mem match {
    case SRAMType => 
      val srams = (0 until numBufs).map{ i => 
        Module(new SRAM(logicalDims, bitWidth, 
                        banks, strides, 
                        combinedXBarWMux, flatXBarRMux,
                        flatDirectWMux, flatDirectRMux,
                        bankingMode, syncMem))
      }
      // Route NBuf IO to SRAM IOs
      srams.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (buffer, portMapping) =>
          val bufferBase = xBarWMux.filter(_._1 < buffer).values.map(_.values).toList.flatten.sum // Index into NBuf io
          val sramXBarWPorts = portMapping.values.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(buffer) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this buffer to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.xBarW(bufferBase + k).en := io.xBarW(bufferBase + k).en & wMask
            f.io.xBarW(bufferBase + k).data := io.xBarW(bufferBase + k).data
            f.io.xBarW(bufferBase + k).ofs := io.xBarW(bufferBase + k).ofs
            f.io.xBarW(bufferBase + k).banks.zip(io.xBarW(bufferBase + k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect DirectW ports
        directWMux.foreach { case (buffer, portMapping) =>
          val bufferBase = directWMux.filter(_._1 < buffer).values.map(_.values).flatten.toList.flatten.length // Index into NBuf io
          val sramDirectWPorts = portMapping.values.flatten.toList.length
          val wMask = Utils.getRetimed(ctrl.io.statesInW(buffer) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this buffer to this sram
          (0 until sramDirectWPorts).foreach {k => 
            f.io.directW(bufferBase + k).en := io.directW(bufferBase + k).en & wMask
            f.io.directW(bufferBase + k).data := io.directW(bufferBase + k).data
            f.io.directW(bufferBase + k).ofs := io.directW(bufferBase + k).ofs
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.values.map(_.values).toList.flatten.sum
          val sramBroadcastWPorts = broadcastWMux.values.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.xBarW(sramXBarWBase + k).en := io.broadcast(k).en
            f.io.xBarW(sramXBarWBase + k).data := io.broadcast(k).data
            f.io.xBarW(sramXBarWBase + k).ofs := io.broadcast(k).ofs
            f.io.xBarW(sramXBarWBase + k).banks.zip(io.broadcast(k).banks).foreach{case (a:UInt,b:UInt) => a := b}
          }
        }

        // Connect XBarR ports
        xBarRMux.foreach { case (buffer, portMapping) =>
          val bufferBase = xBarRMux.filter(_._1 < buffer).values.map(_.values).toList.flatten.sum // Index into NBuf io
          val sramXBarRPorts = portMapping.values.sum
          val rMask = Utils.getRetimed(ctrl.io.statesInR(buffer) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this buffer to this sram
          (0 until sramXBarRPorts).foreach {k => 
            f.io.xBarR(bufferBase + k).en := io.xBarR(bufferBase + k).en & rMask
            f.io.xBarR(bufferBase + k).data := io.xBarR(bufferBase + k).data
            f.io.xBarR(bufferBase + k).ofs := io.xBarR(bufferBase + k).ofs
            f.io.xBarR(bufferBase + k).banks.zip(io.xBarR(bufferBase+k).banks).foreach{case (a:UInt,b:UInt) => a := b}
            f.io.flow(k) := io.flow(k) // Dangerous move here
          }
        }

        // Connect DirectR ports
        directRMux.foreach { case (buffer, portMapping) =>
          val bufferBase = directRMux.filter(_._1 < buffer).values.map(_.values).flatten.toList.flatten.length // Index into NBuf io
          val sramDirectRPorts = portMapping.values.flatten.toList.length
          val rMask = Utils.getRetimed(ctrl.io.statesInR(buffer) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this buffer to this sram
          (0 until sramDirectRPorts).foreach {k => 
            f.io.directR(bufferBase + k).en := io.directR(bufferBase + k).en & rMask
            f.io.directR(bufferBase + k).data := io.directR(bufferBase + k).data
            f.io.directR(bufferBase + k).ofs := io.directR(bufferBase + k).ofs
            f.io.flow(k + {if (hasXBarR) numXBarR else 0}) := io.flow(k + {if (hasXBarR) numXBarR else 0}) // Dangerous move here
          }
        }

      }

      // Connect buffers to output data ports
      (0 until numBufs).foreach {i =>
        val sel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesOut(i) === a.U, {if (Utils.retime) 1 else 0}) }
        (0 until totalOutputs).foreach{ j => 
          io.output.data(i*totalOutputs + j) := chisel3.util.Mux1H(sel, srams.map{f => f.io.output.data(j)})
        }
      }
    case FFType => 
      val ffs = (0 until numBufs).map{ i => 
        Module(new FF(bitWidth, combinedXBarWMux))
      }
      // Route NBuf IO to FF IOs
      ffs.zipWithIndex.foreach{ case (f,i) => 
        // Connect XBarW ports
        xBarWMux.foreach { case (buffer, portMapping) =>
          val bufferBase = xBarWMux.filter(_._1 < buffer).values.map(_.values).toList.flatten.sum // Index into NBuf io
          val sramXBarWPorts = portMapping.values.sum
          val wMask = Utils.getRetimed(ctrl.io.statesInW(buffer) === i.U, {if (Utils.retime) 1 else 0}) // Check if ctrl is routing this buffer to this sram
          (0 until sramXBarWPorts).foreach {k => 
            f.io.input(bufferBase + k).en := io.xBarW(bufferBase + k).en & wMask
            f.io.input(bufferBase + k).data := io.xBarW(bufferBase + k).data
          }
        }

        // Connect Broadcast ports
        if (hasBroadcastW) {
          val sramXBarWBase = xBarWMux.values.map(_.values).toList.flatten.sum
          val sramBroadcastWPorts = broadcastWMux.values.sum
          (0 until sramBroadcastWPorts).foreach {k => 
            f.io.input(sramXBarWBase + k).en := io.broadcast(k).en
            f.io.input(sramXBarWBase + k).data := io.broadcast(k).data
          }
        }
      }

      // Connect buffers to output data ports
      (0 until numBufs).foreach {i =>
        val sel = (0 until numBufs).map{ a => Utils.getRetimed(ctrl.io.statesOut(i) === a.U, {if (Utils.retime) 1 else 0}) }
        io.output.data(i) := chisel3.util.Mux1H(sel, ffs.map{f => f.io.output.data})
      }
  }




  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxPort: Int, vecId: Int) {
    val bufferBase = xBarWMux.filter(_._1 < bufferPort).values.map(_.values).toList.flatten.sum
    val muxBase = xBarWMux(bufferPort).toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarW(bufferBase + muxBase) := wBundle
  }

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxPort: Int, vecId: Int): UInt = {connectXBarRPort(rBundle, bufferPort, muxPort, vecId, true.B)}

  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    val bufferBase = xBarRMux.filter(_._1 < bufferPort).values.map(_.values).toList.flatten.sum
    val muxBase = xBarRMux(bufferPort).toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.xBarR(bufferBase + muxBase) := rBundle    
    io.flow(bufferBase + muxBase) := flow
    io.output.data(bufferBase + vecId)
  }

  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxPort: Int, vecId: Int) {
    val bufferBase = directWMux.filter(_._1 < bufferPort).values.map(_.values).flatten.toList.length 
    val muxBase = directWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.flatten.toList.length + vecId
    io.directW(bufferBase + muxBase) := wBundle
  }

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxPort: Int, vecId: Int): UInt = {connectDirectRPort(rBundle, bufferPort, muxPort, vecId, true.B)}

  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxPort: Int, vecId: Int, flow: Bool): UInt = {
    val bufferBase = directRMux.filter(_._1 < bufferPort).values.map(_.values).flatten.toList.length
    val flowBase = xBarRMux.values.map(_.values).toList.flatten.sum
    val muxBase = directRMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.flatten.toList.length + vecId
    io.directR(bufferBase + muxBase) := rBundle    
    io.flow(flowBase + bufferBase + muxBase) := flow
    io.output.data(vecId)
  }

  def connectBroadcastWPort(wBundle: W_XBar, muxPort: Int, vecId: Int) {
    val muxBase = broadcastWMux.toSeq.sortBy(_._1).toMap.filter(_._1 < muxPort).values.sum + vecId
    io.broadcast(muxBase) := wBundle
  }

  def connectStageCtrl(done: Bool, en: Bool, ports: List[Int]) {
    ports.foreach{ port => 
      io.sEn(port) := en
      io.sDone(port) := done
    }
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



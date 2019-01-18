package fringe.templates.memory

import chisel3._
import chisel3.util.Mux1H
import fringe.templates.math._
import fringe.templates.counters.SingleCounter
import fringe.utils.{log2Up, getRetimed}
import fringe.utils.XMap._
import fringe.utils.DMap._
import fringe.utils.implicits._
import fringe.Ledger
import fringe._

import scala.math.log

object implicits {

	implicit class FixFMAAccumBundleOps(op: FixFMAAccumBundle) {
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool): Seq[UInt] = {
	  	Ledger.connectXBarR(op.hashCode, 0)
	  	Seq(op.output)
	  }
	  def connectXBarWPort(index: Int, data1: UInt, data2: UInt, en: Bool, last: Bool, first: Bool): Unit = {
	  	op.input(index).input1 := data1
	  	op.input(index).input2 := data2
	  	op.input(index).enable := en
	  	op.input(index).last := last
	  	op.input(index).first := first
	  	Ledger.connectXBarW(op.hashCode, index)
	  }
	}

	implicit class MultiArgOutOps(op: MultiArgOut) {
	  def connectXBarR(): UInt = {Ledger.connectXBarR(op.hashCode, 0); op.output.echo}
	  def connectXBarR(p: Int, data: UInt, valid: Bool): Unit = {Ledger.connectXBarW(op.hashCode, p); op.port(p).bits := data; op.port(p).valid := valid}

	}
	implicit class FixOpAccumBundleOps(op: FixOpAccumBundle) {
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool): Seq[UInt] = {
	  	Ledger.connectXBarR(op.hashCode, 0)
	  	Seq(op.output)
	  }
	  def connectXBarWPort(index: Int, data1: UInt, en: Bool, last: Bool, first: Bool): Unit = {
	  	op.input(index).input1 := data1
	  	op.input(index).enable := en
	  	op.input(index).last := last
	  	op.input(index).first := first
	  	Ledger.connectXBarW(op.hashCode, index)
	  }
	}

	implicit class MemInterfaceOps(op: MemInterface) {
	  var usedMuxPorts = List[(String,(Int,Int,Int,Int))]() // Check if the muxPort, muxAddr, lane, castgrp is taken for this connection style (xBar or direct)
	  def connectXBarWPort(wBundle: W_XBar, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
	    assert(op.p.hasXBarW)
	    assert(op.p.xBarWMux.contains((muxAddr._1,muxAddr._2,0)))
	    assert(!usedMuxPorts.contains(("XBarW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to XBarW port $muxAddr twice!")
	    usedMuxPorts ::= ("XBarW", (muxAddr._1,muxAddr._2,0,0))
	    val base = op.p.xBarWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
	    op.xBarW(base) := wBundle
	    Ledger.connectXBarW(op.hashCode, base)
	  }
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectXBarRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
	  def connectXBarRPort(rBundle: R_XBar, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool): Seq[UInt] = {
	    assert(op.p.hasXBarR)
	    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
	      val castgrp = if (ignoreCastInfo) 0 else cg
	      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
	      val base = op.p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).size
	      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
	      val outBase = op.p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).sum - op.p.xBarRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum
	      if (bid == 0) {
	        if (ignoreCastInfo && i == 0) {
	          assert(op.p.xBarRMux.contains((muxAddr._1, effectiveOfs, 0)))
	          assert(!usedMuxPorts.contains(("XBarR", (muxAddr._1,effectiveOfs, i, 0))), s"Attempted to connect to XBarR port $muxAddr, castgrp $castgrp on lane $i twice!")
	          usedMuxPorts ::= ("XBarR", (muxAddr._1,effectiveOfs, i, 0))
	        } else if (!ignoreCastInfo) {
	          assert(op.p.xBarRMux.contains((muxAddr._1, effectiveOfs, castgrp)))
	          assert(!usedMuxPorts.contains(("XBarR", (muxAddr._1,effectiveOfs, i, castgrp))), s"Attempted to connect to XBarR port $muxAddr, castgrp $castgrp on lane $i twice!")
	          usedMuxPorts ::= ("XBarR", (muxAddr._1,effectiveOfs, i, castgrp))
	        }
	        op.xBarR(base).connectLane(vecId, i, rBundle, backpressure)
	      }
	      Ledger.connectXBarR(op.hashCode, base) // Partial connect??
	      // Temp fix for merged readers not recomputing port info
	      op.output.data(outBase + vecId)
	    }
	    
	  }
	  def connectDirectWPort(wBundle: W_Direct, bufferPort: Int, muxAddr: (Int, Int)): Unit = {
	    assert(op.p.hasDirectW)
	    assert(op.p.directWMux.contains((muxAddr._1,muxAddr._2,0)))
	    assert(!usedMuxPorts.contains(("DirectW", (muxAddr._1,muxAddr._2,0,0))), s"Attempted to connect to DirectW port $muxAddr twice!")
	    usedMuxPorts ::= ("DirectW", (muxAddr._1,muxAddr._2,0,0))
	    val base = op.p.directWMux.accessParsBelowMuxPort(muxAddr._1,muxAddr._2,0).size
	    Ledger.connectDirectW(op.hashCode, base)
	    op.directW(base) := wBundle
	  }
	  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean): Seq[UInt] = {connectDirectRPort(rBundle, bufferPort, muxAddr, castgrps, broadcastids, ignoreCastInfo, true.B)}
	  def connectDirectRPort(rBundle: R_Direct, bufferPort: Int, muxAddr: (Int, Int), castgrps: List[Int], broadcastids: List[Int], ignoreCastInfo: Boolean, backpressure: Bool): Seq[UInt] = {
	    assert(op.p.hasDirectR)
	    castgrps.zip(broadcastids).zipWithIndex.map{case ((cg, bid), i) => 
	      val castgrp = if (ignoreCastInfo) 0 else cg
	      val effectiveOfs = if (ignoreCastInfo) muxAddr._2 else muxAddr._2 + i
	      val base = op.p.directRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs, castgrp).size
	      val vecId = if (ignoreCastInfo) i else castgrps.take(i).count(_ == castgrp)
	      val outBase = op.p.directRMux.accessParsBelowMuxPort(muxAddr._1,effectiveOfs,castgrp).sum - op.p.directRMux.accessParsBelowMuxPort(muxAddr._1,0,0).sum
	      if (bid == 0) {
	        if (ignoreCastInfo && i == 0) {
	          assert(op.p.directRMux.contains((muxAddr._1, effectiveOfs, 0)))
	          assert(!usedMuxPorts.contains(("DirectR", (muxAddr._1,effectiveOfs, i, 0))), s"Attempted to connect to DirectR port $muxAddr, castgrp $castgrp on lane $i twice!")
	          usedMuxPorts ::= ("DirectR", (muxAddr._1,effectiveOfs, i, 0))
	        } else if (!ignoreCastInfo) {
	          assert(op.p.directRMux.contains((muxAddr._1, effectiveOfs, castgrp)))
	          assert(!usedMuxPorts.contains(("DirectR", (muxAddr._1,effectiveOfs, i, castgrp))), s"Attempted to connect to DirectR port $muxAddr, castgrp $castgrp on lane $i twice!")
	          usedMuxPorts ::= ("DirectR", (muxAddr._1,effectiveOfs, i, castgrp))
	        }
	        op.directR(base).connectLane(vecId, i, rBundle, backpressure)
	      }
	      // Temp fix for merged readers not recomputing port info
	      Ledger.connectDirectR(op.hashCode, base) // Partial connect?
	      op.output.data(op.p.xBarOutputs + outBase + vecId)
	    }
	  }
	}
}
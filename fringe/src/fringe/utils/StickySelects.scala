package fringe.utils

import chisel3._

//class StickySelects(n: Int, dual_permissive: Boolean = false) extends Module {
//  val io = IO(new Bundle {
//    val ins = Vec(n, Input(Bool()))
//    val outs = Vec(n, Output(Bool()))
//  })
//
//  if (n == 1) io.outs(0) := io.ins(0)
//  else if (n > 1) {
//    val nbits = chisel3.util.log2Up(n)
//	  val outs = Array.tabulate(n){i =>
//      val r = RegInit(false.B)
//      val elseActive = if (dual_permissive) {
//        io.ins.patch(i,Nil,1).map{x => Mux(x, 1.U(nbits.W), 0.U(nbits.W))}.reduce(_+_) > 1.U(nbits.W)
//      } else { io.ins.patch(i,Nil,1).reduce{_||_} }
//      val meActive = io.ins(i)
//      val next = Mux(elseActive, false.B, meActive | r)
//      r := next
//      next
//    }
//	  io.outs.zip(outs).foreach{case(o,i) => o := i}
//  }
//}

class StickySelects(n: Int, dual_permissive: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val ins = Vec(n, Input(Bool()))
    val outs = Vec(n, Output(Bool()))
  })

  if (n == 1) io.outs(0) := io.ins(0)
  else if (n > 1) {
    val nbits = chisel3.util.log2Up(n)
    val outs = Array.tabulate(n){i => RegInit(false.B)}
	  val eagerOuts = Array.tabulate(n){i =>
      val r = outs(i)
      val elseActive = if (dual_permissive) {
        val outs_count = Wire(UInt(nbits.W)); chisel3.core.dontTouch(outs_count)
        outs_count := outs.map{x => Mux(x, 1.U(nbits.W), 0.U(nbits.W))}.reduce{_+_}
        val ins_count = Wire(UInt(nbits.W)); chisel3.core.dontTouch(ins_count)
        ins_count := io.ins.patch(i,Nil,1).map{x => Mux(x, 1.U(nbits.W), 0.U(nbits.W))}.reduce(_+_)
//         io.ins.patch(i,Nil,1).map{x => Mux(x, 1.U(nbits.W), 0.U(nbits.W))}.reduce(_+_) > 1.U(nbits.W)
           outs_count + ins_count > 2.U(nbits.W)
      } else { io.ins.patch(i,Nil,1).reduce{_||_} }
      val meActive = io.ins(i)
      val next = Mux(elseActive, meActive, meActive | r)
      r := next
      next
    }
	  io.outs.zip(eagerOuts).foreach{case(o,i) => o := i}
  }
}

//
//package fringe.utils
//
//import chisel3._
//
//class StickySelects(n: Int, dual_permissive: Boolean = false) extends Module {
//  val io = IO(new Bundle {
//    val ins = Vec(n, Input(Bool()))
//    val outs = Vec(n, Output(Bool()))
//  })
//
//  if (n == 1) io.outs(0) := io.ins(0)
//  else if (n > 1) {
//    val nbits = chisel3.util.log2Up(n)
//	  val outs = Array.tabulate(n){i => RegInit(false.B)}
//    Array.tabulate(n){i =>
//      val r = outs(i)
//      val elseActive = io.ins.patch(i,Nil,1).reduce{_||_}
////      if (dual_permissive) {
////        r && io.ins.patch(i,Nil,1).map{x => Mux(x, 1.U(nbits.W), 0.U(nbits.W))}.reduce(_+_) > 1.U(nbits.W)
////      } else { io.ins.patch(i,Nil,1).reduce{_||_} }
//      val meActive = io.ins(i)
//      val next = Mux(elseActive, meActive, meActive | r)
//      r := next
//      next
//    }
//	  io.outs.zip(outs).foreach{case(o,i) => o := i}
//  }
//}

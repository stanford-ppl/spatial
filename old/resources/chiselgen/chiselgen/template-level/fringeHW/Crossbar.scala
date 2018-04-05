package fringe

import chisel3._
import chisel3.util._

import templates.Utils.log2Up
import scala.language.reflectiveCalls

case class SwitchParams(val numIns: Int, val numOuts: Int)

/**
 * Crossbar config register format
 */
case class CrossbarConfig(p: SwitchParams) extends Bundle {
  var outSelect = Vec(p.numOuts, UInt(log2Up(1+p.numIns).W))

  override def cloneType(): this.type = {
    new CrossbarConfig(p).asInstanceOf[this.type]
  }
}

/**
 * Core logic inside a crossbar
 */
class CrossbarCore[T<:Data](val t: T, val p: SwitchParams, val registerOutput: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val ins = Input(Vec(p.numIns, t.cloneType))
    val outs = Output(Vec(p.numOuts, t.cloneType))
    val config = Input(CrossbarConfig(p))
  })

  io.outs.zipWithIndex.foreach { case(out,i) =>
    val outMux = Module(new MuxN(t, p.numIns))
    for (ii <- 0 until p.numIns) {
      outMux.io.ins(ii) := io.ins(ii)
    }

    outMux.io.sel := io.config.outSelect(i)
    out := (if (registerOutput) RegNext(outMux.io.out, 0.U) else outMux.io.out)
  }
}


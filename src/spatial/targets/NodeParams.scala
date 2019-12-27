package spatial.targets

import argon._
import argon.node._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.util.spatialConfig
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.metadata.memory._
import spatial.metadata.blackbox._
import spatial.metadata.retiming._
import scala.math.log

trait NodeParams {

  @rig def nbits(e: Sym[_]): Int = e.tp match {case Bits(bits) => bits.nbits }
  def sign(e: Sym[_]): Boolean = e.tp match {case FixPtType(s,_,_) => s; case _ => true }

  // TODO[1]: Arbitrary scheduling order
  @stateful def nStages(e: Sym[_]): Int = e.children.length

  @stateful def nodeParams(s: Sym[_], op: Op[_]): (String,Seq[(String,Double)]) = op match {
    case op@RegAccumFMA(_,d,_,_,_) => 
      val log2correction = if (nbits(d) < 6) 1 else 0
      (op.name, Seq("b" -> nbits(d), "layers" -> log(nbits(d) * 0.1875 + 1)/log(2), "drain" -> 1, "correction" -> log2correction))
    case op@RegAccumOp(_,d,_,t,_) => 
      t match {
        case AccumMul => 
          val log2correction = if (nbits(d) < 6) 1 else 0
          (op.name + "Mul", Seq("b" -> nbits(d), "layers" -> log(nbits(d) * 0.1875 + 1)/log(2), "drain" -> 0.1875*nbits(d), "correction" -> log2correction))
        case _ => 
          val log2correction = if (nbits(d) < 32) 1 else 0
          (op.name, Seq("b" -> nbits(d), "layers" -> log(nbits(d) * 0.03125 + 1)/log(2), "drain" -> nbits(d)/32, "correction" -> log2correction))
      }
    case op:FixOp[_,_,_,_] => (op.name, Seq("b" -> op.fmt.nbits))

    case op:FltOp[_,_,_]   => (op.name, Nil)

    case _@DelayLine(d,_) => ("DelayLine", Seq("d" -> {if (s.userInjectedDelay) d else 0}))

    case _:Mux[_] => ("Mux", Seq("b" -> nbits(s)))

    case op@SpatialBlackboxUse(bbox,_) => ("SpatialBlackbox", Seq("lat" -> bbox.bodyLatency.headOption.getOrElse(0.0).toInt))
    case op:VerilogBlackbox[_,_] => ("VerilogBlackbox", Seq("lat" -> s.bboxInfo.latency))

    case _:SRAMRead[_,_] if spatialConfig.enableAsyncMem => ("SRAMAsyncRead", Nil)
    case _:SRAMBankedRead[_,_] if spatialConfig.enableAsyncMem => ("SRAMBankedAsyncRead", Nil)

    case _@RegFileVectorRead(rf,_,_) => ("RegFileVectorRead", Seq("e" -> rf.constDims.product.toDouble))

    case op:Switch[_] if s.isBits => ("SwitchMux", Seq("n" -> op.selects.length, "b" -> nbits(s)))
    case op:Switch[_] => ("Switch", Seq("n" -> op.selects.length))

    case op:CounterNew[_] => ("Counter", Seq("b" -> op.A.nbits, "p" -> op.par.toInt))
    case op:CounterChainNew => ("CounterChain", Seq("n" -> op.counters.length))

    case _ if op.isControl =>
      val style = s.getRawSchedule.getOrElse(Sequenced)
      (style.toString, Seq("n" -> nStages(s)))

    case _ => (op.productPrefix, Nil)
  }


}

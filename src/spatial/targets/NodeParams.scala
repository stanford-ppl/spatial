package spatial.targets

import core._
import forge.tags._
import spatial.data._
import spatial.lang._
import spatial.node._
import spatial.util._
import spatial.internal.spatialConfig

trait NodeParams {

  def nbits(e: Sym[_]): Int = e.tp match {case Bits(bits) => bits.nbits }
  def sign(e: Sym[_]): Boolean = e.tp match {case FixPtType(s,_,_) => s; case _ => true }

  // TODO[1]: Arbitrary scheduling order
  @stateful def nStages(e: Sym[_]): Int = e.children.length

  @stateful def nodeParams(s: Sym[_], op: Op[_]): (String,Seq[(String,Double)]) = op match {
    case op:FixOp[_,_,_,_] => (op.name, Seq("b" -> op.fmt.nbits))

    case op:FltOp[_,_,_]   => (s"${op.name}_${op.fmt.mbits}_${op.fmt.ebits}", Nil)

    case _:Mux[_] => ("Mux", Seq("b" -> nbits(s)))

    case _:SRAMRead[_,_] if spatialConfig.enableAsyncMem => ("SRAMAsyncRead", Nil)
    case _:SRAMBankedRead[_,_] if spatialConfig.enableAsyncMem => ("SRAMBankedAsyncRead", Nil)

    case op:Switch[_] if s.isBits => ("SwitchMux", Seq("n" -> op.selects.length, "b" -> nbits(s)))
    case op:Switch[_] => ("Switch", Seq("n" -> op.selects.length))

    case op:CounterNew[_] => ("Counter", Seq("b" -> op.A.nbits, "p" -> op.par.toInt))
    case op:CounterChainNew => ("CounterChain", Seq("n" -> op.counters.length))

    case _ if isControl(op) =>
      val style = styleOf.get(s).getOrElse(ControlSchedule.Seq)
      (style.toString, Seq("n" -> nStages(s)))

    case _ => (op.productPrefix, Nil)
  }


}

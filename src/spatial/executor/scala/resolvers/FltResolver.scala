package spatial.executor.scala.resolvers

import argon._
import argon.node._
import emul._
import spatial.executor.scala.{EmulResult, ExecutionState, SimpleEmulVal}

trait FltResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {
    case Op(FltIsPosInf(a)) =>
      val result = execState.getValue[FloatPoint](a).isPositiveInfinity
      SimpleEmulVal(emul.Bool(result))

    case Op(FltIsNegInf(a)) =>
      val result = execState.getValue[FloatPoint](a).isNegativeInfinity
      SimpleEmulVal(emul.Bool(result))

    case Op(FltIsNaN(a)) =>
      val result = execState.getValue[FloatPoint](a).isNaN
      SimpleEmulVal(emul.Bool(result))

    case Op(FltToFix(a, f2)) =>
      val result = execState.getValue[FloatPoint](a).toFixedPoint(f2.toEmul)
      SimpleEmulVal(result)

    case Op(FltToFlt(a, f2)) =>
      val result = execState.getValue[FloatPoint](a).toFloatPoint(f2.toEmul)
      SimpleEmulVal(result)

    case Op(FltToText(a, format)) =>
      val result = execState.getValue[FloatPoint](a).toString
      SimpleEmulVal(result)

    case Op(rand@FltRandom(max)) =>
      SimpleEmulVal(max match {
        case Some(maximum) =>
          FloatPoint.random(execState.getValue[FloatPoint](maximum), rand.fmt.toEmul)
        case None =>
          FloatPoint.random(rand.fmt.toEmul)
      })

    case _ => super.run(sym, execState)
  }
}

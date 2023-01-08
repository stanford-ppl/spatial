package spatial.executor.scala.resolvers
import argon._
import argon.node._
import emul._
import spatial.executor.scala.{EmulResult, ExecutionState, SimpleEmulVal}

trait FixResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = sym match {

    case Op(op: FixFMA[_, _, _]) =>
      val m0 = execState.getValue[FixedPoint](op.m0)
      val m1 = execState.getValue[FixedPoint](op.m1)
      val add = execState.getValue[FixedPoint](op.add)
      SimpleEmulVal((m0 * m1) * add)

    case Op(op: FixDivSRA[_, _, _]) =>
      // Pretty sure this one is equivalent to FixSRA
      val a = execState.getValue[FixedPoint](op.a)
      val b = execState.getValue[FixedPoint](op.b)
      if (b >= 0) SimpleEmulVal(a >> b) else SimpleEmulVal(a << -b)

    case Op(op: FixSLA[_, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      val b = execState.getValue[FixedPoint](op.b)
      if (b >= 0) SimpleEmulVal(a << b) else SimpleEmulVal(a >> -b)

    case Op(op: FixSRA[_, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      val b = execState.getValue[FixedPoint](op.b)
      if (b >= 0) SimpleEmulVal(a >> b) else SimpleEmulVal(a << -b)

    case Op(op: FixSRU[_, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      val b = execState.getValue[FixedPoint](op.b)
      if (b >= 0) SimpleEmulVal(a >>> b) else SimpleEmulVal(a << -b)

    case Op(op: FixToFix[_, _, _, _, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      SimpleEmulVal(a.toFixedPoint(op.fmt.toEmul))

    case Op(op: FixToFixSat[_, _, _, _, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      SimpleEmulVal(FixedPoint.saturating(a.value, a.valid, op.fmt.toEmul))

    case Op(op: FixToFixUnb[_, _, _, _, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      SimpleEmulVal(FixedPoint.unbiased(a.value, a.valid, op.fmt.toEmul))

    case Op(op: FixToFixUnbSat[_, _, _, _, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      SimpleEmulVal(FixedPoint.unbiased(a.value, a.valid, op.fmt.toEmul, saturate = true))

    case Op(op: FixToFlt[_, _, _, _, _]) =>
      val a = execState.getValue[FixedPoint](op.a)
      SimpleEmulVal(a.toFloatPoint(op.f2.toEmul))

    case Op(ftt: FixToText[_, _, _]) =>
      val a = execState.getValue[FixedPoint](ftt.a.asSym)
      SimpleEmulVal(a.toString)

    case Op(ttf: TextToFix[_, _, _]) =>
      val a = execState.getValue[String](ttf.t)
      SimpleEmulVal(FixedPoint(a, ttf.f.toEmul))

    case Op(random@FixRandom(max)) =>
      max match {
        case Some(maximum) =>
          SimpleEmulVal(FixedPoint.random(execState.getValue[FixedPoint](maximum), random.fmt.toEmul))
        case None =>
          SimpleEmulVal(FixedPoint.random(random.fmt.toEmul))
      }

    case _ => super.run(sym, execState)
  }

}

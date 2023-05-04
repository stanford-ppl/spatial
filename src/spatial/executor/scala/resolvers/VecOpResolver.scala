package spatial.executor.scala.resolvers

import argon.{Exp, Op}
import argon.lang.{Bits, Struct}
import emul.FixedPoint
import spatial.executor.scala._
import spatial.executor.scala.memories._
import spatial.lang._
import argon.node._
import spatial.metadata.memory._

trait VecOpResolver extends OpResolverBase {
  override def run[U, V](sym: Exp[U, V], op: Op[V], execState: ExecutionState): SomeEmul = {
    implicit def state: argon.State = execState.IR
    op match {
      case va@VecAlloc(elems) =>
        val initValues = elems.map {
          elem =>
            execState(elem.asInstanceOf[argon.Sym[_]])
        }
        SimpleEmulVal(initValues)

      case VecApply(vec, ind) =>
        execState.getValue[Seq[SomeEmul]](vec)(ind)

      case _ => super.run(sym, op, execState)
    }
  }
}

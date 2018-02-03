package pcc.node

import forge._
import pcc.core._
import pcc.data._
import pcc.lang._

/** Nodes with non-zero latency, no internal state, which can be conditionally executed **/
abstract class Primitive[A:Sym] extends AccelOp[A] {
  val isStateless: Boolean = false
}
object Primitive {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Primitive[_]) => Some(x)
    case _ => None
  }
}
object Stateless {
  @stateful def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(p:Primitive[_]) if p.isStateless => Some(x)
    case Expect(c) => Some(x)
    case _ => None
  }
}

abstract class EnPrimitive[A:Sym] extends Primitive[A] {
  def ens: Seq[Bit]
}


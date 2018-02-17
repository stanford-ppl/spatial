package nova.node

import forge.tags._
import forge.tags.stateful
import nova.core._
import nova.data._
import nova.lang._

/** Nodes with non-zero latency, no internal state, which can be conditionally executed **/
abstract class Primitive[R:Type] extends AccelOp[R] {
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

abstract class EnPrimitive[R:Type] extends Primitive[R] {
  def ens: Seq[Bit]
}


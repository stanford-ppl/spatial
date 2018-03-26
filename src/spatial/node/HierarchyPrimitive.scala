package spatial.node

import spatial.lang._

import forge.tags._
import argon._
import spatial.data.Expect

/** Nodes with non-zero latency, no internal state, which can be conditionally executed */
abstract class Primitive[R:Type] extends AccelOp[R] {
  /** If true, has no corresponding resources in hardware.
    * Exists only as a compiler construct for typing purposes, etc.
    *
    * e.g. DataAsBits, BitsAsData
    */
  val isTransient: Boolean = false
}
object Primitive {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Primitive[_]) => Some(x)
    case _ => None
  }
}
object Transient {
  @stateful def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(p:Primitive[_]) if p.isTransient => Some(x)
    case Expect(_) => Some(x)
    case _ => None
  }
}

abstract class EnPrimitive[R:Type] extends Primitive[R] with Enabled[R]


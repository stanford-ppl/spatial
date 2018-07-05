package argon.node

import argon._

/** A DSL operator */
abstract class DSLOp[R:Type] extends Op[R] {
  /** If false, this node may not be supported on hardware accelerators. */
  val canAccel: Boolean = true
}

// TODO: This is a weird thing to have in argon (its somewhat Spatial specific)
/** Nodes with non-zero latency, no internal state, which can be conditionally executed */
abstract class Primitive[R:Type] extends DSLOp[R] {
  /** If true, exists as a compiler construct for typing purposes, etc. */
  val isTransient: Boolean = false
}
object Primitive {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_:Primitive[_]) => Some(x)
    case _ => None
  }
}

abstract class EnPrimitive[R:Type] extends Primitive[R] with Enabled[R]



/** Allocation of any black box */
abstract class Alloc[T:Type] extends DSLOp[T] {

}
object Alloc {
  def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(_: Alloc[_]) => Some(x)
    case _ => None
  }
}

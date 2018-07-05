package spatial.node

import argon._
import argon.node._

import forge.tags._
import spatial.metadata.bounds.Expect

// TODO: This is a bit odd to have Primitive in argon and Transient in spatial
abstract class Transient[R:Type] extends Primitive[R] {
  override val isTransient: Boolean = true
}
object Transient {
  @stateful def unapply(x: Sym[_]): Option[Sym[_]] = x match {
    case Op(p:Primitive[_]) if p.isTransient => Some(x)
    case Expect(_) => Some(x)
    case _ => None
  }
}


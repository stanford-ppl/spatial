package argon.node

import argon._
import argon.lang._
import forge.tags._

@op case class VecAlloc[T:Bits](elems: Seq[T])(implicit val tV: Vec[T]) extends Primitive[Vec[T]] {
  override val isTransient: Boolean = true
}

@op case class VecApply[T:Bits](vec: Vec[T], i: Int) extends Primitive[T] {
  override val isTransient: Boolean = true
  @rig override def rewrite: T = vec match {
    case Op(VecAlloc(elems)) => elems.apply(i)
    case _ => super.rewrite
  }
}
@op case class VecSlice[T:Bits](vec: Vec[T], msw: Int, lsw: Int)(implicit val tV: Vec[T]) extends Primitive[Vec[T]] {
  override val isTransient: Boolean = true
  @rig override def rewrite: Vec[T] = vec match {
    case Op(VecAlloc(elems)) => stage(VecAlloc(elems.slice(lsw, msw+1)))
    case _ => super.rewrite
  }
}

@op case class VecConcat[T:Bits](vecs: Seq[Vec[T]])(implicit val tV: Vec[T]) extends Primitive[Vec[T]] {
  override val isTransient: Boolean = true
  @rig override def rewrite: Vec[T] = {
    if (vecs.forall{case Op(VecAlloc(_)) => true; case _ => false }) {
      val elems = vecs.flatMap{case Op(VecAlloc(e)) => e }
      stage(VecAlloc(elems))
    }
    else super.rewrite
  }
}

@op case class VecReverse[T:Bits](vec: Vec[T])(implicit val tV: Vec[T]) extends Primitive[Vec[T]] {
  override val isTransient: Boolean = true
  @rig override def rewrite: Vec[T] = vec match {
    case Op(VecAlloc(elems)) => stage(VecAlloc(elems.reverse))
    case _ => super.rewrite
  }
}

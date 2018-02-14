package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class VecAlloc[T:Bits](elems: Seq[T])(implicit val tV: Vec[T]) extends Op[Vec[T]]
@op case class VecApply[T:Bits](vec: Vec[T], i: Int) extends Op[T]
@op case class VecSlice[T:Bits](vec: Vec[T], msw: Int, lsw: Int) extends Op[Vec[T]]

@op case class VecConcat[T:Bits](vecs: Seq[Vec[T]])(implicit val tV: Vec[T]) extends Op[Vec[T]]

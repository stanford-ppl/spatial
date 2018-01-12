package pcc.node

import forge._
import pcc.core._
import pcc.lang._

@op case class VecAlloc[T:Bits](elems: Seq[T])(implicit val tV: Vec[T]) extends Op[Vec[T]]

package argon.node

import argon._
import argon.lang._

import forge.tags._

@op case class SeriesForeach[A:Num](start: Num[A], end: Num[A], step: Num[A], func: Lambda1[A,Void])
       extends Op[Void] {
  val A: Num[A] = Num[A]
  override def binds = super.binds + func.input
}

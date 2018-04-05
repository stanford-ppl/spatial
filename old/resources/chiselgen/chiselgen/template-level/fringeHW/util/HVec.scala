// See LICENSE for license details.

package util

import scala.collection.immutable.ListMap
import chisel3._

class HVec[T<:Data](wires: Seq[T]) extends Record with collection.IndexedSeq[T] {
  def apply(x: Int) = wires(x)
  val elements = ListMap(wires.zipWithIndex.map { case (n,i) => (i.toString, n) }:_*)
  def length = wires.length

  override def cloneType: this.type = new HVec(wires.map(_.cloneType)).asInstanceOf[this.type]
}

object HVec {
  def apply[T<:Data](wires: Seq[T]) = new HVec(wires)
  def tabulate[T<:Data](size: Int)(gen: Int => T) = HVec(Seq.tabulate(size) { i => gen(i)})
}

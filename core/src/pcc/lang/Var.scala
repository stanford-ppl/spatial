package pcc.lang

import forge._
import pcc.core._
import pcc.node._

import scala.collection.mutable

case class Var[A:Sym](eid: Int, tA: Sym[A]) extends Sym[Var[A]](eid) {
  type AI = tA.I
  override type I = Array[AI]

  override def fresh(id: Int): Var[A] = Var[A](eid,tA)
  override def isPrimitive: Boolean = false
  override def stagedClass: Class[Var[A]] = classOf[Var[A]]
}
object Var {
  private lazy val types = new mutable.HashMap[Sym[_],Var[_]]()
  implicit def tp[A:Sym]: Var[A] = types.getOrElseUpdate(typ[A], new Var[A](-1,typ[A])).asInstanceOf[Var[A]]

  @api def alloc[A:Sym](init: Option[A]): Var[A] = {
    implicit val tV: Var[A] = Var.tp[A]
    stage(NewVar(init))
  }
  @api def read[A:Sym](v: Var[A]): A = stage(ReadVar(v))
  @api def assign[A:Sym](v: Var[A], x: A): Void = stage(AssignVar(v,x))
}

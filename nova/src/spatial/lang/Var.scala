package spatial.lang

import forge.tags._
import core._
import spatial.node._

import nova.implicits.views._

import scala.collection.mutable

case class Var[A:Type]() extends Ref[Var[A]] {
  val tA: Type[A] = typ[A]
  type AI = tA.I
  override type I = Array[AI]

  override def fresh: Var[A] = new Var[A]
  override def isPrimitive: Boolean = false

  @rig def value: A = Var.read(this)

  @api override def !==(that: Var[A]): Bit = this.value.viewRef nEql that.value.viewRef
  @api override def ===(that: Var[A]): Bit = this.value.viewRef isEql that.value.viewRef
}
object Var {
  private lazy val types = new mutable.HashMap[Type[_],Var[_]]()
  implicit def tp[A:Type]: Var[A] = types.getOrElseUpdate(typ[A], (new Var[A]).asType).asInstanceOf[Var[A]]

  @api def alloc[A:Type](init: Option[A]): Var[A] = {
    implicit val tV: Var[A] = Var.tp[A]
    stage(NewVar(init))
  }
  @api def read[A:Type](v: Var[A]): A = stage(ReadVar(v))
  @api def assign[A:Type](v: Var[A], x: A): Void = stage(AssignVar(v,x))
}

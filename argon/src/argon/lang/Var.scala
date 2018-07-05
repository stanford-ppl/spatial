package argon.lang

import argon._
import forge.tags._
import forge.{AppState, Ptr, VarLike}
import argon.node._

@ref class Var[A:Type] extends Top[Var[A]] with StagedVarLike[A] with Ref[Ptr[Any],Var[A]] {
  val A: Type[A] = Type[A]

  override protected val __neverMutable: Boolean = A.neverMutable

  @rig def __sread(): A = Var.read(this)
  @rig def __sassign(x: A): Unit = Var.assign(this, x)
}
object Var {
  @api def read[A](v: Var[A]): A = stage(VarRead(v)(v.A))
  @api def assign[A](v: Var[A], x: A): Void = stage(VarAssign(v,x)(v.A))
  @api def alloc[A:Type](init: Option[A]): Var[A] = stage(VarNew[A](init))
}

package spatial.lang

import core._
import forge.tags._
import forge.{AppState, Ptr, VarLike}
import spatial.node._

@ref class Var[A:Type] extends Top[Var[A]] with StagedVarLike[A] with Ref[Ptr[Any],Var[A]] {
  val tA: Type[A] = Type[A]

  override protected val __isPrimitive: Boolean = tA.isPrimitive

  @rig def __sread(): A = Var.read(this)
  @rig def __sassign(x: A): Unit = Var.assign(this, x)
}
object Var {
  @api def read[A](v: Var[A]): A = stage(VarRead(v)(v.tA))
  @api def assign[A](v: Var[A], x: A): Void = stage(VarAssign(v,x)(v.tA))
  @api def alloc[A:Type](init: Option[A]): Var[A] = stage(VarNew[A](init))
}

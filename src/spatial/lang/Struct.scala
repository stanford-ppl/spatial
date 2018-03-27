package spatial.lang

import forge.tags._
import argon._
import spatial.node._

trait Struct[A] extends Top[A] with Ref[Nothing,A] {
  override val __isPrimitive = false
  val box: A <:< Struct[A]
  private implicit lazy val A: Struct[A] = this.selfType
  @rig def field[F:Type](name: String): F = Struct.field[A,F](me, name)

  def fields: Seq[(String,ExpType[_,_])]
}

object Struct {
  @rig def apply[S:Struct](elems: (String,Sym[_])*): S = stage(SimpleStruct[S](elems))
  @rig def field[S:Struct,A:Type](struct: S, name: String): A = stage(FieldApply[S,A](struct,name))
  @rig def field_update[S:Struct,A:Type](struct: S, name: String, data: A): Void = stage(FieldUpdate[S,A](struct,name,data))
}

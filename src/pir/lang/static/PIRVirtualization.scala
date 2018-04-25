package pir.lang.static

import forge.VarLike

trait PIRVirtualization /*extends forge.EmbeddedControls {

  def __newVar[T](init: T): VarLike[T] = new forge.Ptr[T](init)
  def __assign[T](lhs: VarLike[T], rhs: T): Unit = lhs.__assign(rhs)
  def __readVar[T](v: VarLike[T]): T = v.__read

  def __ifThenElse[T](cond: Boolean, thenBr: T, elseBr: T): T = macro forge.EmbeddedControls.ifThenElseImpl[T]

  implicit class EqualsOps(x: Any) {
    def infix_!=(y: Any): Boolean = x != y
    def infix_==(y: Any): Boolean = x == y
  }

  def infix_+(x1: String, x2: Any): String = macro forge.EmbeddedControls.string_+
  def infix_toString(x: Any): String = macro forge.EmbeddedControls.any_toString
}*/


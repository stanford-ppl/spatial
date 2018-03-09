package nova.compiler

import core._
import spatial.lang.Bit

trait NovaVirtualization extends forge.EmbeddedControls {
  //def __newVar[T](init: T): T =
  //def __readVar[T](v: T): T =
  //def __assign[T](lhs: T, rhs: T): Unit =

  override def __valName(init: Any, name: String): Unit = init match {
    case s: Sym[_] => s.name = Some(name)
    case _ => super.__valName(init, name)
  }

  /** Control structures */
  //def __ifThenElse[T](cond: Boolean, thenBr: T, elseBr: T): T =
  //def __return(expr: Any): Nothing =
  //def __whileDo(cond: Boolean, body: Unit): Unit =
  //def __doWhile(body: Unit, cond: Boolean): Unit =

  //def __throw(t: Throwable): Unit =

//  /** `Any` Infix Methods */
//  def infix_+(x1: String, x2: Any): String =
//  def infix_+(x1: Any, x2: Any): Any =
//  def infix_==(x1: Any, x2: Any): Boolean =
//  def infix_!=(x1: Any, x2: Any): Boolean =
//  def infix_##(x: Any): Int =
//  def infix_equals(x1: Any, x2: Any): Boolean =
//  def infix_hashCode(x: Any): Int =
//  def infix_asInstanceOf[T](x: Any): T =
//  def infix_isInstanceOf[T](x: Any): Boolean =
//  def infix_toString(x: Any): String =
//  def infix_getClass(x: Any): Class[_] =
//
//  /** `AnyRef` Infix Methods */
//  def infix_eq(x1: AnyRef, x2: AnyRef): Boolean =
//  def infix_ne(x1: AnyRef, x2: AnyRef): Boolean =
//  def infix_notify(x: AnyRef): Unit =
//  def infix_notifyAll(x: AnyRef): Unit =
//  def infix_synchronized[T](x: AnyRef, body: T): T =
//  def infix_wait(x: AnyRef): Unit =
//  def infix_wait(x: AnyRef, timeout: Long): Unit =
//  def infix_wait(x: AnyRef, timeout: Long, nanos: Int): Unit =
//  def infix_clone(x: AnyRef): AnyRef =
//  def infix_finalize(x: AnyRef): Unit =
//
//  // Define universal arithmetic for all primitive types
//  def infix_+[A<:AnyVal, B<:AnyVal](lhs: A, rhs: B): AnyVal =
}

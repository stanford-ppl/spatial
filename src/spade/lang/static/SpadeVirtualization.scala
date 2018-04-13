package spade.lang.static

import language.experimental.macros
import scala.reflect.macros.whitebox

trait SpadeVirtualization extends forge.EmbeddedControls {

  def __ifThenElse[T](cond: Boolean, thenBr: T, elseBr: T): T = macro forge.EmbeddedControls.ifThenElseImpl[T]

  implicit class EqualsOps(x: Any) {
    def !==(y: Any): Boolean = x != y
    def ===(y: Any): Boolean = x == y
  }

  def infix_+(x1: String, x2: Any): String = macro forge.EmbeddedControls.string_+

}

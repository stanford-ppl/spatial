package spatial.lang
package types

import forge.tags._
import core._

abstract class Num[A](implicit ev: A<:<Num[A]) extends Order[A] {
  private implicit lazy val tA: Num[A] = this.tp.view(this)
  //def numI: Numeric[I]
  @api def unary_-(): A
  @api def +(that: A): A
  @api def -(that: A): A
  @api def *(that: A): A
  @api def /(that: A): A
  @api def %(that: A): A

  @rig def neg(a: A): A = -a
  @rig def add(a: A, b: A): A = a + b
  @rig def sub(a: A, b: A): A = a - b
  @rig def mul(a: A, b: A): A = a * b
  @rig def div(a: A, b: A): A = a / b
  @rig def mod(a: A, b: A): A = a % b

  @rig def abs(a: A): A
  @rig def ceil(a: A): A
  @rig def floor(a: A): A
  @rig def pow(b: A, e: A): A
  @rig def exp(a: A): A
  @rig def ln(a: A): A
  @rig def sqrt(a: A): A
  @rig def sin(a: A): A
  @rig def cos(a: A): A
  @rig def tan(a: A): A
  @rig def sinh(a: A): A
  @rig def cosh(a: A): A
  @rig def tanh(a: A): A
  @rig def asin(a: A): A
  @rig def acos(a: A): A
  @rig def atan(a: A): A
  @rig def sigmoid(a: A): A
}


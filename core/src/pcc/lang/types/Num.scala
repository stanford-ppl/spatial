package pcc.lang
package types

import forge._

abstract class Num[A](implicit ev: A<:<Num[A]) extends Bits[A] {
  //def numI: Numeric[I]
  @api def unary_-(): A
  @api def +(that: A): A
  @api def -(that: A): A
  @api def *(that: A): A
  @api def /(that: A): A
  @api def %(that: A): A

  @api def neg(a: A): A = -a
  @api def add(a: A, b: A): A = a + b
  @api def sub(a: A, b: A): A = a - b
  @api def mul(a: A, b: A): A = a * b
  @api def div(a: A, b: A): A = a / b
  @api def mod(a: A, b: A): A = a % b
}


package pcc
package ir
package typeclasses

import forge._

abstract class Num[A](id: Int)(implicit ev: A<:<Num[A]) extends Bits[A](id) {
  @api def +(that: A): A
  @api def -(that: A): A
  @api def *(that: A): A
  @api def /(that: A): A

  @api def add(a: A, b: A): A = a + b
  @api def sub(a: A, b: A): A = a - b
  @api def mul(a: A, b: A): A = a * b
  @api def div(a: A, b: A): A = a / b
}


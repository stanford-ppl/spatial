package pcc
package ir

case class Mux[T:Bits](s: Bit, a: T, b: T) extends Op[T]
case class FixMin[T:Fix](a: T, b: T) extends Op[T]
case class FixMax[T:Fix](a: T, b: T) extends Op[T]
case class FltMin[T:Flt](a: T, b: T) extends Op[T]
case class FltMax[T:Flt](a: T, b: T) extends Op[T]




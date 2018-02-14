package pcc.lang.static

trait BitsMethods {
  def tbits[T:Bits]: Bits[T] = implicitly[Bits[T]]
  def mbits[A,B](x: Bits[A]): Bits[B] = x.asInstanceOf[Bits[B]]

  def zero[A:Bits]: A = tbits[A].zero
  def one[A:Bits]: A = tbits[A].one
}

package nova.lang.static

import forge.tags._
import nova.core._
import nova.node.BitsAsData

trait BitsMethods {
  def tbits[T:Bits]: Bits[T] = implicitly[Bits[T]]
  def mbits[A,B](x: Bits[A]): Bits[B] = x.asInstanceOf[Bits[B]]

  @api def zero[A:Bits]: A = tbits[A].zero
  @api def one[A:Bits]: A = tbits[A].one

  @api def random[A:Bits]: A = tbits[A].random(None)
  @api def random[A:Bits](max: A): A = tbits[A].random(Some(max))

  implicit class BitVectorOps(vec: Vec[Bit]) {
    /**
      * Gives a view of this vector of bits as the given type.
      */
    @api def recast[A:Bits]: A = {
      Bits.checkMismatch(vec.tp.extract,tbits[A], "recastUnchecked")
      vec.recastUnchecked[A]
    }

    /**
      * Gives a view of this vector of bits as the given type without length mismatch warnings.
      */
    @api def recastUnchecked[A:Bits]: A = stage(BitsAsData[A](vec, tbits[A]))
  }
}

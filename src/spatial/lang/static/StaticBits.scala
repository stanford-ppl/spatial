package spatial.lang
package static

import forge.tags._
import forge.util.overloads._
import core._

import spatial.node.{BitsAsData,Mux}

trait StaticBits { this: SpatialStatics =>
  @api def mux[A:Bits](s: Bit, a: Lift[A], b: Lift[A]): A = {
    stage(Mux(s,a.unbox,b.unbox))
  }
  /*@api def mux[A,B](s: Bit, a: A, b: B)(implicit ov: Overload1): Vec[Bit] = {
    val nBits = Math.max(Bits[A].nBits, Bits[B].nBits)
    implicit val tV: Vec[Bit] = Vec.tp[Bit](nBits)
    val bitsA = a.as[Vec[Bit]]
    val bitsB = b.as[Vec[Bit]]
    stage(Mux(s,bitsA,bitsB))
  }*/

  @api def zero[A:Bits]: A = Bits[A].zero
  @api def one[A:Bits]: A = Bits[A].one

  @api def random[A:Bits]: A = Bits[A].random(None)
  @api def random[A:Bits](max: A): A = Bits[A].random(Some(max))

  implicit class BitVectorOps(vec: Vec[Bit]) {
    /**
      * Gives a view of this vector of bits as the given type.
      */
    @rig def recast[A:Bits]: A = {
      Bits.checkMismatch(vec.selfType, Bits[A], "recastUnchecked")
      vec.recastUnchecked[A]
    }

    /**
      * Gives a view of this vector of bits as the given type without length mismatch warnings.
      */
    @rig def recastUnchecked[A:Bits]: A = stage(BitsAsData[A](vec, Bits[A]))
  }
}

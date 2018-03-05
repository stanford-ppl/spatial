package spatial.lang
package static

import forge.tags._
import core._

import spatial.node.{BitsAsData,Mux,OneHotMux}

trait StaticBits { this: SpatialStatics =>
  @api def mux[A](s: Bit, a: Bits[A], b: Bits[A]): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s,a,b))
  }
  @api def mux[A,B](s: Bit, a: Bits[A], b: Lift[B]): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s, a, tA.from(b.orig)))
  }
  @api def mux[A,B](s: Bit, a: Lift[A], b: Bits[B]): B = {
    implicit val tB: Bits[B] = b.selfType
    stage(Mux(s, tB.from(a.orig), b))
  }
  @api def mux[A:Bits](s: Bit, a: Lift[A], b: Lift[A]): A = {
    stage(Mux(s,a.unbox,b.unbox))
  }

  @api def oneHotMux[A:Bits](sels: Seq[Bit], vals: Seq[A]): A = {
    stage(OneHotMux(sels,vals.map{s => boxBits(s) }))
  }


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

package spatial.lang
package static

import forge.tags._
import argon._

import spatial.node.{BitsAsData,Mux,OneHotMux}

trait StaticBits { this: SpatialStatics =>
  @api def mux[A](s: Bit, a: Bits[A], b: Bits[A]): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s,a,b))
  }
  @api def mux[A](s: Bit, a: Bits[A], b: Literal): A = {
    implicit val tA: Bits[A] = a.selfType
    stage(Mux(s, a, tA.from(b.value)))
  }
  @api def mux[A](s: Bit, a: Literal, b: Bits[A]): A = {
    implicit val tB: Bits[A] = b.selfType
    stage(Mux(s, tB.from(a.value), b))
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
    @rig def recastUnchecked[A:Bits]: A = Bits[A] match {
      case struct: Struct[_] =>
        val fieldNames = struct.fields.map{case (name,_) => name }
        val fieldTypes = struct.fields.map{case (_, mT) => mT }
        val sizes = struct.fields.map{case (_, Bits(bT)) => bT.nbits }
        val offsets = List.tabulate(sizes.length){i => sizes.drop(i+1).sum }

        val fields = (fieldTypes,offsets).zipped.toSeq.collect{case (b: Bits[_],offset) =>
          b.boxed(vec.sliceUnchecked(msb = offset+b.nbits, lsb = offset).asType(b))
        }
        val namedFields = fieldNames.zip(fields)

        implicit val sT: Struct[A] = struct.asInstanceOf[Struct[A]]
        Struct[A](namedFields:_*)

      case v: Vec[_] => Vec.fromBits(vec, v.width, v.A).asInstanceOf[A]
      case _ =>
        stage(BitsAsData[A](vec, Bits[A]))
    }
  }
}

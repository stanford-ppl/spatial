package spatial.lang

import core._
import forge.tags._
import spatial.node._

abstract class DRAM[A:Bits,C[T]](implicit val evMem: C[A] <:< DRAM[A,C]) extends Top[C[A]] with RemoteMem[A,C] {
  val tA: Bits[A] = Bits[A]
  def rank: Int

  @api override def neql(that: C[A]): Bit = {
    error(src, "Native comparison of DRAMs is unsupported. Use getMem to extract data.")
    error(src)
    super.neql(that)
  }
  @api override def eql(that: C[A]): Bit = {
    error(src, "Native comparision of DRAMs is unsupported. Use getMem to extract data.")
    error(src)
    super.eql(that)
  }
}
object DRAM {
  /** Allocates a 1-dimensional [[DRAM1]] with capacity of `length` elements of type [[A]]. **/
  @api def apply[A:Bits](length: I32): DRAM1[A] = stage(DRAMNew[A,DRAM1](Seq(length)))

  /** Allocates a 2-dimensional [[DRAM2]] with `rows` x `cols` elements of type [[A]]. **/
  @api def apply[A:Bits](rows: I32, cols: I32): DRAM2[A] = stage(DRAMNew[A,DRAM2](Seq(rows,cols)))

  /** Allocates a 3-dimensional [[DRAM3]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32): DRAM3[A] = stage(DRAMNew[A,DRAM3](Seq(d0,d1,d2)))

  /** Allocates a 4-dimensional [[DRAM4]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32): DRAM4[A] = stage(DRAMNew[A,DRAM4](Seq(d0,d1,d2,d3)))

  /** Allocates a 5-dimensional [[DRAM5]] with the given dimensions and elements of type [[A]]. **/
  @api def apply[A:Bits](d0: I32, d1: I32, d2: I32, d3: I32, d4: I32): DRAM5[A] = stage(DRAMNew[A,DRAM5](Seq(d0,d1,d2,d3,d4)))

}

/** A 1-dimensional [[DRAM]] with elements of type [[A]]. **/
@ref class DRAM1[A:Bits] extends DRAM[A,DRAM1] with Ref[Array[Any],DRAM1[A]] {
  def rank: Int = 1
}

/** A 2-dimensional [[DRAM]] with elements of type [[A]]. **/
@ref class DRAM2[A:Bits] extends DRAM[A,DRAM2] with Ref[Array[Any],DRAM2[A]] {
  def rank: Int = 2
}

/** A 3-dimensional [[DRAM]] with elements of type [[A]]. **/
@ref class DRAM3[A:Bits] extends DRAM[A,DRAM3] with Ref[Array[Any],DRAM3[A]] {
  def rank: Int = 3
}

/** A 4-dimensional [[DRAM]] with elements of type [[A]]. **/
@ref class DRAM4[A:Bits] extends DRAM[A,DRAM4] with Ref[Array[Any],DRAM4[A]] {
  def rank: Int = 4
}

/** A 5-dimensional [[DRAM]] with elements of type [[A]]. **/
@ref class DRAM5[A:Bits] extends DRAM[A,DRAM5] with Ref[Array[Any],DRAM5[A]] {
  def rank: Int = 5
}


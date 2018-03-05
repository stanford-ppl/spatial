package spatial.lang

import core._
import forge.tags._
import spatial.node._

@ref class DRAM[A:Bits] extends Top[DRAM[A]] with RemoteMem[A,DRAM] with Ref[Array[Any],DRAM[A]] {
  val tA: Bits[A] = Bits[A]
  val evMem: DRAM[A] <:< RemoteMem[A,DRAM] = implicitly[DRAM[A] <:< RemoteMem[A,DRAM]]

  @api override def neql(that: DRAM[A]): Bit = {
    error(src, "Native comparison of DRAMs is unsupported. Use getMem to extract data.")
    error(src)
    super.neql(that)
  }
  @api override def eql(that: DRAM[A]): Bit = {
    error(src, "Native comparision of DRAMs is unsupported. Use getMem to extract data.")
    error(src)
    super.eql(that)
  }
}
object DRAM {
  @api def apply[A:Bits](dims: I32*): DRAM[A] = stage(DRAMNew(dims))
}

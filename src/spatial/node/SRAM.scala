package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class SRAMNew[A:Bits](dims: Seq[I32]) extends MemAlloc[SRAM[A]]

@op case class SRAMRead[A:Bits](
    mem:  SRAM[A],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Reader[A,A]

@op case class SRAMWrite[A:Bits](
    mem:  SRAM[A],
    data: Bits[A],
    addr: Seq[Idx],
    ens: Set[Bit])
  extends Writer[A]




/** A banked read of a vector from an [[SRAM]]
  * @param mem the SRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SRAMBankedRead[T:Bits](
    mem:  SRAM[T],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[T]])
  extends BankedReader[T]

/** A banked write of a vector to an [[SRAM]]
  * @param mem the SRAM being written
  * @param data the vector of data being written to the SRAM
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SRAMBankedWrite[T:Bits](
    mem:  SRAM[T],
    data: Seq[Sym[T]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]])
  extends BankedWriter[T]
package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class SRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
    )(implicit tp: Type[C[A]])
  extends MemAlloc[C[A]]

@op case class SRAMRead[A:Bits,C[T]](
    mem:  SRAM[A,C],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Reader[A,A]

@op case class SRAMWrite[A:Bits,C[T]](
    mem:  SRAM[A,C],
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
@op case class SRAMBankedRead[A:Bits,C[T]](
    mem:  SRAM[A,C],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends BankedReader[A]

/** A banked write of a vector to an [[SRAM]]
  * @param mem the SRAM being written
  * @param data the vector of data being written to the SRAM
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SRAMBankedWrite[A:Bits,C[T]](
    mem:  SRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]
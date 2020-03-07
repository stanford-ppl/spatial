package spatial.node

import argon._
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of SRAM with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class SRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
    )(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from an SRAM.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class SRAMRead[A:Bits,C[T]](
    mem:  SRAM[A,C],
    addr: Seq[ICTR],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to an SRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class SRAMWrite[A:Bits,C[T]](
    mem:  SRAM[A,C],
    data: Bits[A],
    addr: Seq[ICTR],
    ens: Set[Bit])
  extends Writer[A]




/** A banked read of a vector of elements from an SRAM.
  * @param mem the SRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SRAMBankedRead[A:Bits,C[T]](
    mem:  SRAM[A,C],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends BankedReader[A]

/** A banked write of a vector of elements to an SRAM.
  * @param mem the SRAM being written
  * @param data the vector of data being written to the SRAM
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SRAMBankedWrite[A:Bits,C[T]](
    mem:  SRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]

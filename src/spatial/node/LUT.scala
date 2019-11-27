package spatial.node

import argon._
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of LUT with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class LUTNew[A:Bits,C[T]](
  dims:  Seq[I32],
  elems: Seq[Bits[A]]
)(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** An allocation of a subclass C of LUT with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class FileLUTNew[A:Bits,C[T]](
  dims:  Seq[I32],
  path: String 
)(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from an LUT.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class LUTRead[A:Bits,C[T]](
  mem:  LUT[A,C],
  addr: Seq[Idx],
  ens:  Set[Bit])
  extends Reader[A,A]


/** A banked read of a vector of elements from an LUT.
  * @param mem the LUT being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class LUTBankedRead[A:Bits,C[T]](
  mem:  LUT[A,C],
  bank: Seq[Seq[Idx]],
  ofs:  Seq[Idx],
  enss: Seq[Set[Bit]]
)(implicit val vT: Type[Vec[A]])
  extends BankedReader[A]

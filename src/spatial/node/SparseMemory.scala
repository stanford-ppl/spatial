package spatial.node

import argon._
import argon.node.{Alloc, Primitive}
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of SparseSRAM with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class SparseSRAMNew[A:Bits,C[T]](
    dims: Seq[I32],
    )(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

@op case class SparseDRAMNew[A:Bits,C[T]](
    dims: Seq[I32],
    par: Int
    )(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from an SparseSRAM.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class SparseSRAMRead[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    addr: Seq[Idx],
    barriers: Seq[BarrierTransaction],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to an SparseSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class SparseSRAMWrite[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    barriers: Seq[BarrierTransaction],
    ens: Set[Bit])
  extends Writer[A]


/** Write of a single element to an SparseSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param op The operation for RMW
  * @param order The order for RMW
  * @param ens Associated write enable(s)
  */
@op case class SparseSRAMRMW[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    op: String,
    order: String,
    barriers: Seq[BarrierTransaction],
    remoteAddr:Boolean,
    ens: Set[Bit])
  extends RMWDoer[A,A]


/** A banked read of a vector of elements from an SRAM.
  * @param mem the SRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SparseSRAMBankedRead[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    barriers: Seq[BarrierTransaction],
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
@op case class SparseSRAMBankedWrite[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    barriers: Seq[BarrierTransaction],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]


/** Write of a single element to an SparseSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param op The operation for RMW
  * @param order The order for RMW
  * @param enss Associated write enable(s)
  */
@op case class SparseSRAMBankedRMW[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    op: String,
    order: String,
    barriers: Seq[BarrierTransaction],
    remoteAddr:Boolean,
    enss: Seq[Set[Bit]])(implicit val vT: Type[Vec[A]])
  extends BankedRMWDoer[A]

package spatial.node

import argon._
import argon.node.{Alloc, Primitive}
import forge.tags._
import spatial.lang._
//
//@op case class LockDRAMHostNew[A:Bits,C[T]](dims: Seq[I32], zero: A)(implicit tp: Type[C[A]]) extends DRAMNew[A,C]
//
//@op case class LockDRAMAddress[A:Bits,C[T]](dram: LockDRAM[A,C]) extends Primitive[I64] {
//  val A: Bits[A] = Bits[A]
//}
//
///** Read of a single element from an LockSRAM.
//  * @param mem The memory instance being read
//  * @param addr The N-dimensional address
//  * @param ens Associated read enable(s)
//  */
//@op case class LockDRAMRead[A:Bits,C[T]](
//    mem:  LockDRAM[A,C],
//    addr: Seq[Idx],
//    lock: Option[LockWithKeys[I32]],
//    ens:  Set[Bit])
//  extends Reader[A,A]
//
///** Write of a single element to an LockDRAM.
//  * @param mem The memory instance being written
//  * @param data The element being written
//  * @param addr The N-dimensional address
//  * @param ens Associated write enable(s)
//  */
//@op case class LockDRAMWrite[A:Bits,C[T]](
//    mem:  LockDRAM[A,C],
//    data: Bits[A],
//    addr: Seq[Idx],
//    lock: Option[LockWithKeys[I32]],
//    ens: Set[Bit])
//  extends Writer[A]
//
///** A banked read of a vector of elements from an DRAM.
//  * @param mem the DRAM being read
//  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
//  * @param ofs the bank offset for each vector element
//  * @param enss the set of enables for each vector element
//  */
//@op case class LockDRAMBankedRead[A:Bits,C[T]](
//    mem:  LockDRAM[A,C],
//    bank: Seq[Seq[Idx]],
//    ofs:  Seq[Idx],
//    lock: Option[Seq[LockWithKeys[I32]]],
//    enss: Seq[Set[Bit]]
//    )(implicit val vT: Type[Vec[A]])
//  extends BankedReader[A]
//
///** A banked write of a vector of elements to an DRAM.
//  * @param mem the DRAM being written
//  * @param data the vector of data being written to the DRAM
//  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
//  * @param ofs the bank offset for each vector element
//  * @param enss the set of enables for each vector element
//  */
//@op case class LockDRAMBankedWrite[A:Bits,C[T]](
//    mem:  LockDRAM[A,C],
//    data: Seq[Sym[A]],
//    bank: Seq[Seq[Idx]],
//    ofs:  Seq[Idx],
//    lock: Option[Seq[LockWithKeys[I32]]],
//    enss: Seq[Set[Bit]])
//  extends BankedWriter[A]

/** An allocation of a subclass C of SparseSRAM with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class SparseSRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
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
    ens: Set[Bit])
  extends Writer[A]

/** Write of a single element to an SparseSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class SparseSRAMTokenWrite[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    ens: Set[Bit])
  extends TokenWriter[A]

@op case class SparseSRAMTokenRead[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    addr: Seq[Idx],
    token: Option[Token],
    ens: Set[Bit])
  extends TokenReader[A,A]

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
    token: Option[Token],
    op: String,
    order: String,
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
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends BankedReader[A]

/** A banked read of a vector of elements from an SRAM.
  * @param mem the SRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class SparseSRAMBankedTokenRead[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    tokens: Option[Token],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends BankedTokenReader[A]

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
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]

@op case class SparseSRAMBankedTokenWrite[A:Bits,C[T]](
    mem:  SparseSRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]])
  extends BankedTokenWriter[A]

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
    tokens: Option[Token],
    op: String,
    order: String,
    enss: Seq[Set[Bit]])(implicit val vT: Type[Vec[A]])
  extends BankedRMWDoer[A]

@op case class TokenAnd[A:Bits](a: Token, b: Token) extends Primitive[Token]
@op case class TokenOr[A:Bits](a: Token, b: Token) extends Primitive[Token]
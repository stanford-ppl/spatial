package spatial.node

import argon._
import argon.node.{Alloc, Primitive}
import forge.tags._
import spatial.lang._

@op case class LockDRAMHostNew[A:Bits,C[T]](dims: Seq[I32], zero: A)(implicit tp: Type[C[A]]) extends DRAMNew[A,C]

@op case class LockDRAMAddress[A:Bits,C[T]](dram: LockDRAM[A,C]) extends Primitive[I64] {
  val A: Bits[A] = Bits[A]
}

/** Read of a single element from an LockSRAM.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class LockDRAMRead[A:Bits,C[T]](
    mem:  LockDRAM[A,C],
    addr: Seq[ICTR],
    lock: Option[LockWithKeys[ICTR]],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to an LockDRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class LockDRAMWrite[A:Bits,C[T]](
    mem:  LockDRAM[A,C],
    data: Bits[A],
    addr: Seq[ICTR],
    lock: Option[LockWithKeys[ICTR]],
    ens: Set[Bit])
  extends Writer[A]

/** A banked read of a vector of elements from an DRAM.
  * @param mem the DRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class LockDRAMBankedRead[A:Bits,C[T]](
    mem:  LockDRAM[A,C],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    lock: Option[Seq[LockWithKeys[ICTR]]],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends BankedReader[A]

/** A banked write of a vector of elements to an DRAM.
  * @param mem the DRAM being written
  * @param data the vector of data being written to the DRAM
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class LockDRAMBankedWrite[A:Bits,C[T]](
    mem:  LockDRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    lock: Option[Seq[LockWithKeys[ICTR]]],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]

/** An allocation of a subclass C of LockSRAM with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class LockSRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
    )(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from an LockSRAM.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class LockSRAMRead[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    addr: Seq[ICTR],
    lock: Option[LockWithKeys[ICTR]],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to an LockSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class LockSRAMWrite[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    data: Bits[A],
    addr: Seq[ICTR],
    lock: Option[LockWithKeys[ICTR]],
    ens: Set[Bit])
  extends Writer[A]


/** A banked read of a vector of elements from an SRAM.
  * @param mem the SRAM being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element. Vecor[Dims[]]
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class LockSRAMBankedRead[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    lock: Option[Seq[LockWithKeys[ICTR]]],
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
@op case class LockSRAMBankedWrite[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[ICTR]],
    ofs:  Seq[ICTR],
    lock: Option[Seq[LockWithKeys[ICTR]]],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]


/** An allocation of a Lock node.
  * @param depth Width of the locking module
  */
@op case class LockNew[A:Bits](depth: I32)(implicit val tp: Type[A])
  extends MemAlloc[A,Lock] {
    def dims = Seq(depth)
  }

@op case class LockOnKeys[A:Bits](lock: Lock[A], keys: Seq[A])(implicit val tp: Type[A])
    extends Alloc[LockWithKeys[A]]
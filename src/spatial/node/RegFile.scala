package spatial.node

import argon._
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of RegFile with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class RegFileNew[A:Bits,C[T]](
    dims:  Seq[I32],
    inits: Option[Seq[Bits[A]]]
    )(implicit cT: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from a RegFile.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class RegFileRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to a RegFile.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class RegFileWrite[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Writer[A]

/** Shift of a single element into a 1-dimensional slice of RegFile
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address identifying the location of the 1-dimensional slice
  * @param ens Associated write enable(s)
  * @param axis The axis of the 1-dimensional slice
  *             e.g. for a 2D RegFile:
  *             axis=0: shift into the specified row (through the row)
  *             axis=1: shift into the specified column (through the column)
  */
@op case class RegFileShiftIn[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    ens:  Set[Bit],
    axis: Int)
  extends EnqueuerLike[A]

/** Shift of single elements into a RegFile
  * @param mem The memory instance being written
  * @param data The elements being written
  * @param adr The N-dimensional address identifying the entry plane
  * @param enss Associated write enable(s)
  * @param axis The axis of the 1-dimensional slice
  *             e.g. for a 2D RegFile:
  *             axis=0: shift into the specified row
  *             axis=1: shift into the specified column
  */
@op case class RegFileBankedShiftIn[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Seq[Sym[A]],
    adr:  Seq[Seq[Idx]],
    enss: Seq[Set[Bit]],
    axis: Int
) extends BankedEnqueue[A] {
    override def bank: Seq[Seq[Idx]] = adr
}

/** Reset of a RegFile.
  * @param mem The memory instance being reset
  * @param ens Associated enable(s)
  */
@op case class RegFileReset[A:Bits,C[T]](
    mem: RegFile[A,C],
    ens: Set[Bit])
  extends Resetter[A]

/** Shift of a vector of element into a 1-dimensional slice of RegFile
  * @param mem The memory instance being written
  * @param data The element being written
  * @param adr The N-dimensional address identifying the location of the 1-dimensional slice
  * @param ens Associated write enable(s)
  * @param axis The axis of the 1-dimensional slice
  *             e.g. for a 2D RegFile:
  *             axis=0: shift into the specified row (through the row)
  *             axis=1: shift into the specified column (through the column)
  */
@op case class RegFileShiftInVector[A:Bits,C[T]](
  mem:  RegFile[A,C],
  data: Vec[A],
  adr:  Seq[Idx],
  ens:  Set[Bit],
  axis: Int
) extends VectorEnqueuer[A] {
    override def addr: Seq[Idx] = adr
}

/** A banked read of a vector of elements from a RegFile.
  * @param mem the RegFile being read
  * @param addr a vector of multi-dimensional addresses
  * @param enss the set of enables for each vector element
  */
@op case class RegFileVectorRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    addr: Seq[Seq[Idx]],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Type[Vec[A]])
  extends VectorReader[A] 

/** A banked write of a vector of elements to an RegFile.
  * @param mem the RegFile being written
  * @param data the vector of data being written to the RegFile
  * @param addr a vector of multi-dimensional addresses
  * @param enss the set of enables for each vector element
  */
@op case class RegFileVectorWrite[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Seq[Sym[A]],
    addr: Seq[Seq[Idx]],
    enss: Seq[Set[Bit]])
  extends VectorWriter[A]


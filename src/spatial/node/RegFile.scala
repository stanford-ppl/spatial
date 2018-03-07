package spatial.node

import core._
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of [[RegFile]] with elements of type [[A]].
  * @param dims The dimensions of the memory
  */
@op case class RegFileNew[A:Bits,C[T]](
    dims:  Seq[I32],
    inits: Option[Seq[A]]
    )(implicit cT: Type[C[A]])
  extends MemAlloc[C[A]]

/** Read of a single element from a [[RegFile]].
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class RegFileRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to a [[RegFile]].
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

/** Shift of a single element into a 1-dimensional slice of [[RegFile]]
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address identifying the 1-dimensional slice
  * @param ens Associated write enable(s)
  * @param axis The axis of the 1-dimensional slice
  *             e.g. for a 2D RegFile:
  *             axis=0: shift into the specified row
  *             axis=1: shift into the specified column
  */
@op case class RegFileShiftIn[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    ens:  Set[Bit],
    axis: Int)
  extends EnqueuerLike[A]


/** Reset of a [[RegFile]].
  * @param mem The memory instance being reset
  * @param ens Associated enable(s)
  */
@op case class RegFileReset[A:Bits,C[T]](
    mem: RegFile[A,C],
    ens: Set[Bit])
  extends Resetter[A]


//@op case class RegFileVectorShiftIn[A:Bits](
//  mem:  RegFile[A],
//  data: Vec[T],
//  addr: Seq[Idx],
//  ens:  Set[Bit],
//  ax:   Int,
//  len:  Int
//) extends VectorEnqueuer[A]

/** A banked read of a vector of elements from a [[RegFile]].
  * @param mem the RegFile being read
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class RegFileBankedRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Vec[A])
  extends BankedReader[A]

/** A banked write of a vector of elements to an [[RegFile]].
  * @param mem the RegFile being written
  * @param data the vector of data being written to the RegFile
  * @param bank the (optionally multi-dimensional) bank address(es) for each vector element
  * @param ofs the bank offset for each vector element
  * @param enss the set of enables for each vector element
  */
@op case class RegFileBankedWrite[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]


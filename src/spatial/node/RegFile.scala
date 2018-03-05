package spatial.node

import core._
import forge.tags._
import spatial.lang._

@op case class RegFileNew[A:Bits,C[T]](
    dims:  Seq[I32],
    inits: Option[Seq[A]]
    )(implicit cT: Type[C[A]])
  extends MemAlloc[C[A]]

@op case class RegFileRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    addr: Seq[Idx],
    ens:  Set[Bit])
  extends Reader[A,A]

@op case class RegFileWrite[A:Bits,C[T]](
    mem:  RegFile[A,C],
    addr: Seq[Idx],
    data: Bits[A],
    ens:  Set[Bit])
  extends Writer[A]

@op case class RegFileReset[A:Bits,C[T]](
    mem: RegFile[A,C],
    ens: Set[Bit])
  extends Resetter[A]

@op case class RegFileShiftIn[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    ens:  Set[Bit],
    ax:   Int)
  extends EnqueuerLike[A]

//@op case class RegFileVectorShiftIn[A:Bits](
//  mem:  RegFile[A],
//  data: Vec[T],
//  addr: Seq[Idx],
//  ens:  Set[Bit],
//  ax:   Int,
//  len:  Int
//) extends VectorEnqueuer[A]

@op case class RegFileBankedRead[A:Bits,C[T]](
    mem:  RegFile[A,C],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]]
    )(implicit val vT: Vec[A])
  extends BankedReader[A]

@op case class RegFileBankedWrite[A:Bits,C[T]](
    mem:  RegFile[A,C],
    data: Seq[Sym[A]],
    bank: Seq[Seq[Idx]],
    ofs:  Seq[Idx],
    enss: Seq[Set[Bit]])
  extends BankedWriter[A]


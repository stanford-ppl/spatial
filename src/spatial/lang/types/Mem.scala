package spatial.lang.types

import argon._
import forge.tags._
import spatial.lang._
import spatial.node.{DenseTransfer, MemDenseAlias, SparseTransfer}

trait Mem[A,C[_]] extends Top[C[A]] with Ref[Any,C[A]] {
  val evMem: C[A] <:< Mem[A,C]
  implicit val A: Bits[A]

  override protected val __isPrimitive: Boolean = false
}

trait RemoteMem[A,C[_]] extends Mem[A,C] {
  val evMem: C[A] <:< RemoteMem[A,C]

}

trait LocalMem[A,C[_]] extends Mem[A,C] {
  val evMem: C[A] <:< LocalMem[A,C]
  private implicit val evv: C[A] <:< Mem[A,C] = evMem

  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void
  @rig def __reset(ens: Set[Bit]): Void
}
trait LocalMem0[A,C[T]<:LocalMem0[T,C]] extends LocalMem[A,C]
trait LocalMem1[A,C[T]<:LocalMem1[T,C]] extends LocalMem[A,C] {
  private implicit def C: Type[C[A]] = this.selfType

  /** Create a dense burst load from the given region of DRAM to this on-chip memory. */
  @api def load(dram: DRAM1[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))

  /** Creates a sparse gather from the given region of DRAM to this on-chip memory. */
  @api def gather(dram: DRAMSparseTile[A]): Void = stage(SparseTransfer(dram,me,isGather=true))
}
trait LocalMem2[A,C[T]<:LocalMem2[T,C]] extends LocalMem[A,C] {
  private implicit def C: Type[C[A]] = this.selfType

  /** Create a dense burst load from the given region of DRAM to this on-chip memory. */
  @api def load(dram: DRAM2[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))
}
trait LocalMem3[A,C[T]<:LocalMem3[T,C]] extends LocalMem[A,C] {
  private implicit def C: Type[C[A]] = this.selfType

  /** Create a dense burst load from the given region of DRAM to this on-chip memory. */
  @api def load(dram: DRAM1[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))
}
trait LocalMem4[A,C[T]<:LocalMem4[T,C]] extends LocalMem[A,C] {
  private implicit def C: Type[C[A]] = this.selfType

  /** Create a dense burst load from the given region of DRAM to this on-chip memory. */
  @api def load(dram: DRAM4[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))
}
trait LocalMem5[A,C[T]<:LocalMem5[T,C]] extends LocalMem[A,C] {
  private implicit def C: Type[C[A]] = this.selfType

  /** Create a dense burst load from the given region of DRAM to this on-chip memory. */
  @api def load(dram: DRAM5[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))
}


trait Mem1[A,M1[T]] extends Mem[A,M1] {
  private implicit def M1: Type[M1[A]] = this.selfType

  /** Returns a view of this memory at the addresses in the given `range`. */
  @api def apply(range: Rng): M1[A] = stage(MemDenseAlias[A,M1,M1](me,Seq(range)))
}

trait Mem2[A,M1[T],M2[T]] extends Mem[A,M2] {
  protected def M1: Type[M1[A]]
  private implicit def M1Type: Type[M1[A]] = M1
  private implicit def M2Type: Type[M2[A]] = this.selfType

  /** Creates a view of a dense slice of a row of this memory. Use the * wildcard to view the entire row. */
  @api def apply(row: Idx, cols: Rng): M1[A] = stage(MemDenseAlias[A,M2,M1](me,Seq(row.toSeries, cols)))

  /** Creates a view of a dense slice of a column of this memory. Use the * wildcard to view the entire column. */
  @api def apply(rows: Rng, col: Idx): M1[A] = stage(MemDenseAlias[A,M2,M1](me,Seq(rows, col.toSeries)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(rows: Rng, cols: Rng): M2[A] = stage(MemDenseAlias[A,M2,M2](me,Seq(rows, cols)))
}

trait Mem3[A,M1[T],M2[T],M3[T]] extends Mem[A,M3] {
  protected def M1: Type[M1[A]]
  protected def M2: Type[M2[A]]
  private implicit def M1Type: Type[M1[A]] = M1
  private implicit def M2Type: Type[M2[A]] = M2
  private implicit def M3Type: Type[M3[A]] = this.selfType

  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(r0: Idx, r1: Idx, r2: Rng): M1[A] = stage(MemDenseAlias[A,M3,M1](me,Seq(r0.toSeries,r1.toSeries,r2)))

  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(r0: Idx, r1: Rng, r2: Idx): M1[A] = stage(MemDenseAlias[A,M3,M1](me,Seq(r0.toSeries,r1,r2.toSeries)))

  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(r0: Rng, r1: Idx, r2: Idx): M1[A] = stage(MemDenseAlias[A,M3,M1](me,Seq(r0,r1.toSeries,r2.toSeries)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(r0: Idx, r1: Rng, r2: Rng): M2[A] = stage(MemDenseAlias[A,M3,M2](me,Seq(r0.toSeries,r1,r2)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(r0: Rng, r1: Idx, r2: Rng): M2[A] = stage(MemDenseAlias[A,M3,M2](me,Seq(r0,r1.toSeries,r2)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(r0: Rng, r1: Rng, r2: Idx): M2[A] = stage(MemDenseAlias[A,M3,M2](me,Seq(r0,r1,r2.toSeries)))

  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(r0: Rng, r1: Rng, r2: Rng): M3[A] = stage(MemDenseAlias[A,M3,M3](me,Seq(r0,r1,r2)))
}

trait Mem4[A,M1[T],M2[T],M3[T],M4[T]] extends Mem[A,M4] {
  protected def M1: Type[M1[A]]
  protected def M2: Type[M2[A]]
  protected def M3: Type[M3[A]]
  private implicit def M1Type: Type[M1[A]] = M1
  private implicit def M2Type: Type[M2[A]] = M2
  private implicit def M3Type: Type[M3[A]] = M3
  private implicit def M4Type: Type[M4[A]] = this.selfType

  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Idx, r: Idx, c: Rng): M1[A] = stage(MemDenseAlias[A,M4,M1](me, Seq(q.toSeries, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Idx, r: Rng, c: Idx): M1[A] = stage(MemDenseAlias[A,M4,M1](me, Seq(q.toSeries, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Rng, r: Idx, c: Idx): M1[A] = stage(MemDenseAlias[A,M4,M1](me, Seq(q.toSeries, p, r.toSeries, c.toSeries)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Idx, r: Idx, c: Idx): M1[A] = stage(MemDenseAlias[A,M4,M1](me, Seq(q, p.toSeries, r.toSeries, c.toSeries)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Idx, r: Rng, c: Rng): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q.toSeries, p.toSeries, r, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Idx, r: Idx, c: Rng): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Rng, r: Idx, c: Idx): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q, p, r.toSeries, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Rng, r: Idx, c: Rng): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q.toSeries, p, r.toSeries, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Idx, r: Rng, c: Idx): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Rng, r: Rng, c: Idx): M2[A] = stage(MemDenseAlias[A,M4,M2](me, Seq(q.toSeries, p, r, c.toSeries)))

  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(q: Idx, p: Rng, r: Rng, c: Rng): M3[A] = stage(MemDenseAlias[A,M4,M3](me, Seq(q.toSeries, p, r, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Idx, r: Rng, c: Rng): M3[A] = stage(MemDenseAlias[A,M4,M3](me, Seq(q, p.toSeries, r, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Rng, r: Idx, c: Rng): M3[A] = stage(MemDenseAlias[A,M4,M3](me, Seq(q, p, r.toSeries, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Rng, r: Rng, c: Idx): M3[A] = stage(MemDenseAlias[A,M4,M3](me, Seq(q, p, r, c.toSeries)))

  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(q: Rng, p: Rng, r: Rng, c: Rng): M4[A] = stage(MemDenseAlias[A,M4,M4](me, Seq(q, p, r, c)))
}

trait Mem5[A,M1[T],M2[T],M3[T],M4[T],M5[T]] extends Mem[A,M5] {
  protected def M1: Type[M1[A]]
  protected def M2: Type[M2[A]]
  protected def M3: Type[M3[A]]
  protected def M4: Type[M4[A]]
  private implicit def M1Type: Type[M1[A]] = M1
  private implicit def M2Type: Type[M2[A]] = M2
  private implicit def M3Type: Type[M3[A]] = M3
  private implicit def M4Type: Type[M4[A]] = M4
  private implicit def M5Type: Type[M5[A]] = this.selfType

  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Idx, r: Idx, c: Rng): M1[A] = stage(MemDenseAlias[A,M5,M1](me, Seq(x.toSeries, q.toSeries, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Idx, r: Rng, c: Idx): M1[A] = stage(MemDenseAlias[A,M5,M1](me, Seq(x.toSeries, q.toSeries, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Rng, r: Idx, c: Idx): M1[A] = stage(MemDenseAlias[A,M5,M1](me, Seq(x.toSeries, q.toSeries, p, r.toSeries, c.toSeries)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Idx, r: Idx, c: Idx): M1[A] = stage(MemDenseAlias[A,M5,M1](me, Seq(x.toSeries, q, p.toSeries, r.toSeries, c.toSeries)))
  /** Creates a view of a 1-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Idx, r: Idx, c: Idx): M1[A] = stage(MemDenseAlias[A,M5,M1](me, Seq(x, q.toSeries, p.toSeries, r.toSeries, c.toSeries)))

  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Idx, r: Rng, c: Rng): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q.toSeries, p.toSeries, r, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Rng, r: Idx, c: Rng): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q.toSeries, p, r.toSeries, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Rng, r: Rng, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q.toSeries, p, r, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Idx, r: Idx, c: Rng): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Idx, r: Rng, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Rng, r: Idx, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x.toSeries, q, p, r.toSeries, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Idx, r: Idx, c: Rng): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x, q.toSeries, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Idx, r: Rng, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x, q.toSeries, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Rng, r: Idx, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x, q.toSeries, p, r.toSeries, c.toSeries)))
  /** Creates a view of a 2-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Idx, r: Idx, c: Idx): M2[A] = stage(MemDenseAlias[A,M5,M2](me, Seq(x, q, p.toSeries, r.toSeries, c.toSeries)))

  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Idx, p: Rng, r: Rng, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x.toSeries, q.toSeries, p, r, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Idx, r: Rng, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x.toSeries, q, p.toSeries, r, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Rng, r: Idx, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x.toSeries, q, p, r.toSeries, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Rng, r: Rng, c: Idx): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x.toSeries, q, p, r, c.toSeries)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Idx, r: Rng, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q.toSeries, p.toSeries, r, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Rng, r: Idx, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q.toSeries, p, r.toSeries, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Rng, r: Rng, c: Idx): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q.toSeries, p, r, c.toSeries)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Idx, r: Idx, c: Rng): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q, p.toSeries, r.toSeries, c)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Idx, r: Rng, c: Idx): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q, p.toSeries, r, c.toSeries)))
  /** Creates a view of a 3-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Rng, r: Idx, c: Idx): M3[A] = stage(MemDenseAlias[A,M5,M3](me, Seq(x, q, p, r.toSeries, c.toSeries)))

  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(x: Idx, q: Rng, p: Rng, r: Rng, c: Rng): M4[A] = stage(MemDenseAlias[A,M5,M4](me, Seq(x.toSeries, q, p, r, c)))
  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Idx, p: Rng, r: Rng, c: Rng): M4[A] = stage(MemDenseAlias[A,M5,M4](me, Seq(x, q.toSeries, p, r, c)))
  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Idx, r: Rng, c: Rng): M4[A] = stage(MemDenseAlias[A,M5,M4](me, Seq(x, q, p.toSeries, r, c)))
  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Rng, r: Idx, c: Rng): M4[A] = stage(MemDenseAlias[A,M5,M4](me, Seq(x, q, p, r.toSeries, c)))
  /** Creates a view of a 4-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Rng, r: Rng, c: Idx): M4[A] = stage(MemDenseAlias[A,M5,M4](me, Seq(x, q, p, r, c.toSeries)))

  /** Creates a view of a 5-dimensional, dense region of this memory. */
  @api def apply(x: Rng, q: Rng, p: Rng, r: Rng, c: Rng): M5[A] = stage(MemDenseAlias[A,M5,M5](me, Seq(x, q, p, r, c)))
}

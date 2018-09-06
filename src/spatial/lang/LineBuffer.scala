package spatial.lang

import argon._
import forge.tags._
import spatial.node._

import scala.collection.mutable.Queue

@ref class LineBuffer[A:Bits] extends Top[LineBuffer[A]]
         with LocalMem2[A,LineBuffer]
         with Ref[Queue[Any],LineBuffer[A]] {
  val A: Bits[A] = Bits[A]
  val evMem: LineBuffer[A] <:< LocalMem[A,LineBuffer] = implicitly[LineBuffer[A] <:< LocalMem[A,LineBuffer]]

  // --- Typeclass Methods
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = stage(LineBufferRead(this,addr,ens))
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = stage(LineBufferEnq(this,data,ens))
  @rig def __reset(ens: Set[Bit]): Void = void

  /** Creates a load port to this LineBuffer at the given `row` and `col`. **/
  @api def apply(row: I32, col: I32): A = stage(LineBufferRead(this, Seq(row, col), Set.empty))

  /** Load 1D DRAM into row of LineBuffer. */
  @api def load(dram: DRAM1[A]): Void = stage(DenseTransfer(dram,me,isLoad = true))

}

object LineBuffer {
  @api def apply[A:Bits](rows: I32, cols: I32): LineBuffer[A] = stage(LineBufferNew(rows,cols))
}

//   /** Creates a vectorized load port to this LineBuffer at the given `row` and `cols`. **/
//   @api def apply(row: Index, cols: Range)(implicit ctx: SrcCtx): Vector[T] = {
//     // UNSUPPORTED: Strided range apply of line buffer
//     cols.step.map(_.s) match {
//       case None | Some(Const(1)) =>
//       case _ => error(ctx, "Unsupported stride in LineBuffer apply")
//     }

//     val start  = cols.start.map(_.s).getOrElse(int32s(0))
//     val length = cols.length
//     val exp = LineBuffer.col_slice(s, row.s, start, length.s)
//     exp.tp.wrapped(exp)
//   }
//   /** Creates a vectorized load port to this LineBuffer at the given `rows` and `col`. **/
//   @api def apply(rows: Range, col: Index)(implicit ctx: SrcCtx): Vector[T] = {
//     // UNSUPPORTED: Strided range apply of line buffer
//     rows.step.map(_.s) match {
//       case None | Some(Const(1)) =>
//       case _ => error(ctx, "Unsupported stride in LineBuffer apply")
//     }

//     val start = rows.start.map(_.s).getOrElse(int32s(0))
//     val length = rows.length
//     val exp = LineBuffer.row_slice(s, start, length.s, col.s)
//     exp.tp.wrapped(exp)
//   }

//   /** Creates an enqueue (write) port of `data` to this LineBuffer. **/
//   @api def enq(data: T): MUnit = MUnit(LineBuffer.enq(this.s, data.s, Bit.const(true)))
//   /** Creates an enqueue (write) port of `data` to this LineBuffer, enabled by `en`. **/
//   @api def enq(data: T, en: Bit): MUnit = MUnit(LineBuffer.enq(this.s, data.s, en.s))

//   /** Creates an enqueue port of `data` to this LineBuffer which rotates when `row` changes. **/
//   @api def enq(row: Index, data: T): MUnit = MUnit(LineBuffer.rotateEnq(this.s, row.s, data.s, Bit.const(true)))
//   /** Creates an enqueue port of `data` to this LineBuffer enabled by `en` which rotates when `row` changes. **/
//   @api def enq(row: Index, data: T, en: Bit): MUnit = MUnit(LineBuffer.rotateEnq(this.s, row.s, data.s, en.s))

//   /** Creates a dense transfer from the given region of DRAM to this on-chip memory. **/
//   @api def load(dram: DRAMDenseTile1[T]): MUnit = DRAMTransfers.dense_transfer(dram, this, isLoad = true)

//   /** Creates a dense transfer from the given region of DRAM to this on-chip memory. **/
//   @api def load(dram: DRAMDenseTile2[T]): MUnit = DRAMTransfers.dense_transfer(dram, this, isLoad = true)
// }

// object LineBuffer {
//   /** Allocates a LineBuffer with given `rows` and `cols`.
//     * The contents of this LineBuffer are initially undefined.
//     * `rows` and `cols` must be statically determinable integers.
//     */
//   @api def apply[T:Type:Bits](rows: Index, cols: Index): LineBuffer[T] = wrap(alloc[T](rows.s, cols.s, int32s(1)))

//   /**
//     * Allocates a LineBuffer with given number of `rows` and `cols`, and with given `stride`.
//     * The contents of this LineBuffer are initially undefined.
//     * `rows`, `cols`, and `stride` must be statically determinable integers.
//     */
//   @api def strided[T:Type:Bits](rows: Index, cols: Index, stride: Index): LineBuffer[T] = wrap(alloc[T](rows.s, cols.s, stride.s))

//   implicit def lineBufferType[T:Type:Bits]: Type[LineBuffer[T]] = LineBufferType(typ[T])
//   implicit def linebufferIsMemory[T:Type:Bits]: Mem[T, LineBuffer] = new LineBufferIsMemory[T]


//   @internal def alloc[T:Type:Bits](rows: Exp[Index], cols: Exp[Index], verticalStride: Exp[Index]) = {
//     stageMutable(LineBufferNew[T](rows, cols, verticalStride))(ctx)
//   }

//   @internal def col_slice[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     row:        Exp[Index],
//     colStart:   Exp[Index],
//     length:     Exp[Index]
//   ) = {
//     implicit val vT = length match {
//       case Final(c) => VectorN.typeFromLen[T](c.toInt)
//       case _ =>
//         error(ctx, "Cannot create parametrized or dynamically sized line buffer slice")
//         VectorN.typeFromLen[T](0)
//     }
//     stageUnique(LineBufferColSlice(linebuffer, row, colStart, length))(ctx)
//   }

//   @internal def row_slice[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     rowStart:   Exp[Index],
//     length:     Exp[Index],
//     col:        Exp[Index]
//   ) = {
//     implicit val vT = length match {
//       case Final(c) => VectorN.typeFromLen[T](c.toInt)
//       case _ =>
//         error(ctx, "Cannot create parametrized or dynamically sized line buffer slice")
//         VectorN.typeFromLen[T](0)
//     }
//     stageUnique(LineBufferRowSlice(linebuffer, rowStart, length, col))(ctx)
//   }

//   @internal def load[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     row:        Exp[Index],
//     col:        Exp[Index],
//     en:         Exp[Bit]
//   ) = {
//     stageWrite(linebuffer)(LineBufferLoad(linebuffer,row,col,en))(ctx)
//   }

//   @internal def enq[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     data:       Exp[T],
//     en:         Exp[Bit]
//   ) = {
//     stageWrite(linebuffer)(LineBufferEnq(linebuffer,data,en))(ctx)
//   }

//   @internal def rotateEnq[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     row:        Exp[Index],
//     data:       Exp[T],
//     en:         Exp[Bit]
//   ) = {
//     stageWrite(linebuffer)(LineBufferRotateEnq(linebuffer,row,data,en))(ctx)
//   }

//   @internal def par_load[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     rows:       Seq[Exp[Index]],
//     cols:       Seq[Exp[Index]],
//     ens:        Seq[Exp[Bit]]
//   ) = {
//     implicit val vT = VectorN.typeFromLen[T](ens.length)
//     stageWrite(linebuffer)(ParLineBufferLoad(linebuffer,rows,cols,ens))(ctx)
//   }

//   @internal def par_enq[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     data:       Seq[Exp[T]],
//     ens:        Seq[Exp[Bit]]
//   ) = {
//     stageWrite(linebuffer)(ParLineBufferEnq(linebuffer,data,ens))(ctx)
//   }

//   @internal def par_rotateEnq[T:Type:Bits](
//     linebuffer: Exp[LineBuffer[T]],
//     row:        Exp[Index],
//     data:       Seq[Exp[T]],
//     ens:        Seq[Exp[Bit]]
//   ) = {
//     stageWrite(linebuffer)(ParLineBufferRotateEnq(linebuffer,row,data,ens))(ctx)
//   }

// }

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
  @rig def __read(addr: Seq[Idx], ens: Set[Bit]): A = stage(LineBufferRead(this,addr.map(_.asInstanceOf[ICTR]),ens))
  @rig def __write(data: A, addr: Seq[Idx], ens: Set[Bit]): Void = if (addr.size == 1) stage(LineBufferEnq(this,data,Seq(0.to[ICTR]) ++ addr.map(_.asInstanceOf[ICTR]),ens)) else stage(LineBufferEnq(this,data,addr.map(_.asInstanceOf[ICTR]),ens))
  @rig def __reset(ens: Set[Bit]): Void = void

  /** Creates a load port to this LineBuffer at the given `row` and `col`. **/
  @api def apply(row: I32, col: I32): A = stage(LineBufferRead(this, Seq(row.to[ICTR], col.to[ICTR]), Set.empty))

  /** Creates an enqueue (write) port to this LineBuffer of `data`. **/
  @api def enqAt(row: Idx, data: A): Void = this.enqAt(row, data, true)

  /** Creates a conditional enqueue (write) port to this LineBuffer of `data` enabled by `en`. **/
  @api def enqAt(row: Idx, data: A, en: Bit): Void = stage(LineBufferEnq(this,data,Seq(row.asInstanceOf[ICTR], 0.to[ICTR]), Set(en)))

  /** Load 1D DRAM into row of LineBuffer. */
  @api def load(dram: DRAM1[A]): Void = {
    stage(DenseTransfer(dram,me,isLoad = true))
  }

}

object LineBuffer {
  @api def apply[A:Bits](rows: I32, cols: I32): LineBuffer[A] = stage(LineBufferNew(rows,cols,1))
  @api def strided[A:Bits](rows: I32, cols: I32, stride: I32): LineBuffer[A] = stage(LineBufferNew(rows,cols,stride))
}

package spatial.transform.stream

import argon._
import argon.lang._
import argon.transform.{ForwardTransformer, MutateTransformer}
import spatial.lang.{FIFO, FIFOReg}
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.metadata.memory._

case class FIFOInitializer(IR: State) extends ForwardTransformer with AccelTraversal {
  def stageFIFOInits[A](block: Block[A]): Block[A] = {
    stageScope(block.inputs, block.options) {
      // Stage initializers for the fifos inside
      val fifos = block.stms.filter(_.isFIFO).filter(_.fifoInits.isDefined)
      val fPressure = FIFO[Bit](I32(1))
      stage(UnitPipe(Set.empty, stageBlock {
        val isFirst = fPressure.isEmpty
        fPressure.enq(Bit(true), !isFirst)
        fifos foreach {
          case fifo:FIFO[_] =>
            type T = fifo.A.R
            val inits = fifo.fifoInits.get
            val data = inits.map(_.unbox).asInstanceOf[Seq[T]]
            val fifoSize = fifo.constDims.head
            assert(fifoSize >= data.size, s"FIFO was too small to initialize ($fifoSize < ${data.size})")

            implicit def bEV: Bits[T] = fifo.A
            val dataVec = Vec.fromSeq(data)
            fifo.enqVec(dataVec, isFirst)
        }
        spatial.lang.void
      }, None))
      block.stms.foreach(visit)
      f(block.result)
    }
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = (rhs match {
    case ctrl: Control[_] =>
      ctrl.blocks.filter(_.stms.exists(_.fifoInits.isDefined)) foreach {
        blk =>
          dbgs(s"Transforming $lhs -> $blk")
          register(blk -> stageFIFOInits(blk))
      }
      super.transform(lhs, rhs)
    case _ => super.transform(lhs, rhs)
  }).asInstanceOf[Sym[A]]
}

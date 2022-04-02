package spatial.transform.stream

import argon._
import argon.lang._
import argon.transform.ForwardTransformer
import spatial.dsl.retimeGate
import spatial.lang.FIFO
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.metadata.memory._

case class FIFOInitializer(IR: State) extends ForwardTransformer with AccelTraversal {
  def stageFIFOInits[A](block: Block[A]): Block[A] = {
    stageScope(block.inputs, block.options) {
      // Stage initializers for the fifos inside
      val fifos = block.stms.filter(_.isFIFO).filter(_.fifoInits.isDefined)
      fifos.foreach(visit)
      val fPressure = FIFO[Bit](I32(1))
      fPressure.explicitName = s"RunOnceFIFO"
      stage(UnitPipe(Set.empty, stageBlock {
        val isFirst = fPressure.isEmpty
        fifos foreach {
          case fifo:FIFO[_] =>
            type T = fifo.A.R
            val inits = fifo.fifoInits.get
            val data = inits.map(_.unbox).asInstanceOf[Seq[T]]
            val fifoSize = fifo.constDims.head
            assert(fifoSize >= data.size, s"FIFO was too small to initialize ($fifoSize < ${data.size})")

            implicit def bEV: Bits[T] = fifo.A
            val dataVec = Vec.fromSeq(data)
            f(fifo).enqVec(dataVec, isFirst)
        }
        retimeGate()
        fPressure.enq(Bit(true), !isFirst)
        spatial.lang.void
      }, None))
      block.stms.filterNot(fifos.contains).foreach(visit)
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

package spatial.transform

import argon._
import argon.node._
import argon.lang.Bit
import spatial.libdsl.UInt8
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._
import spatial.util.spatialConfig
import spatial.traversal.AccelTraversal
import spatial.traversal.BlkTraversal

import scala.collection.{mutable => mut}
import spatial.metadata.PendingUses
import spatial.metadata.access._

// This pass removes everything `Circ` related from the IR by splitting up controllers containing `CircApply` nodes into
// pairs of controllers which enqueue to/dequeue from a controller which implements the behavior of the applied `Circ`.
// It must be run after `PipeInserter` because it assumes that all `CircApply` nodes are scoped to an inner controller.

case class CircDesugaring(IR: State) extends MutateTransformer with AccelTraversal {
  private val KILL_CHANNEL_DEPTH = 8;
  private val INPUT_DEPTH = 8;
  private val OUTPUT_DEPTH = 8;

  private val circImpls: mut.Map[Circ[_, _], CircImpl[_, _]] = mut.HashMap.empty

  case class CircImpl[A: Bits, B: Bits](
//     killChannel: FIFO[Boolean],
     inputs: Seq[FIFO[A]],
     outputs: Seq[FIFO[B]],
     processor: Block[Void]
  )

  def getCircImpl[A: Bits, B: Bits](app: CircApply[A, B])(implicit ctx: SrcCtx) = {
//    circImpls.getOrElseUpdate(app.circ, {
//      val n = app.circ.getNumApps
//      val killChannel = FIFO[Bit](KILL_CHANNEL_DEPTH)
//      val inputs = Range(0, n).map(_ => FIFO[A](INPUT_DEPTH)).toList
//      val outputs = Range(0, n).map(_ => FIFO[B](OUTPUT_DEPTH)).toList
//      val processor = {
//        Pipe {
//          val break = Reg[Bit](false)
//          val count = Reg[UInt8](0)
//          Sequential(breakWhen = break).Foreach(*) { _ =>
//            val x = inputs.head.deq()
//            val y = app.circ.func(x)
//            outputs.head.enq(y)
//          }
//        }
//      }
//      CircImpl(inputs, outputs, processor)
//    }).asInstanceOf[CircImpl[A, B]]
  }

  override def transform[A: Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel { super.transform(lhs,rhs) }
    case _: BlackboxImpl[_, _, _] => inBox { super.transform(lhs,rhs) }
    case ctrl: Control[_] => {
      val stms = ctrl.bodies.flatMap(_.blocks).flatMap(_._2.stms)
      val apps = stms.filter(_.op.exists(_.isInstanceOf[CircApply[_, _]])).toSet

      val (firstUsedApp, useIdx) = (stms
        .map(_.inputs.find(apps(_)))
        .zipWithIndex
        .collectFirst{ case (Some(s), i) => (s, i) }
        .get)

//      val app = firstUsedApp.op.get.asInstanceOf[CircApply[_, _]]
//      implicit def aEv: Bits[app.circ.InT] = app.circ.aEv
//      implicit def bEv: Bits[app.circ.OutT] = app.circ.bEv
//      // val impl = getCircImpl(app.asInstanceOf[CircApply[app.circ.InT, app.circ.OutT]])
//      val inst = app.asInstanceOf[CircApply[app.circ.InT, app.circ.OutT]]
//      val impl = getCircImpl(inst)
      val (left, right) = stms.splitAt(useIdx)
      val enqCtrlStms = left.filter(!apps(_))
      val deqCtrlStms = right.filter(!apps(_))

      dbgs(s"CTRL $ctrl ===>\n\t$left\n\t$right")
      super.transform(lhs,rhs)
    }
    case _ => super.transform(lhs,rhs)
  }
}

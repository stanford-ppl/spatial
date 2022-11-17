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
// It must be run after `PipeInserter` because it assumes that `CircApply` results are scoped to an inner controller.

case class CircDesugaring(IR: State) extends MutateTransformer with AccelTraversal {
  private val KILL_CHANNEL_DEPTH = 8;
  private val INPUT_DEPTH = 8;
  private val OUTPUT_DEPTH = 8;

  private val circImpls: mut.Map[Sym[_],CircImpl[_,_]] = mut.HashMap.empty

  case class CircImpl[A:Bits,B:Bits](
//     killChannel: FIFO[Boolean],
     inputs: Seq[FIFO[A]],
     outputs: Seq[FIFO[B]],
//     processor: Block[Void]
  )

  def getCircImpl[A:Bits,B:Bits](app: CircApply[A,B]): CircImpl[A,B] =
    circImpls(app.circ).asInstanceOf[CircImpl[A,B]]

  def createCircImpl(lhs: Circ[_,_], rhs: CircNew[_,_]): Unit = {
    implicit val evA: Bits[rhs.A] = rhs.evA
    implicit val evB: Bits[rhs.B] = rhs.evB

    val n = lhs.getNumApps
    val inputs = Range(0,n).map(_ => FIFO[rhs.A](INPUT_DEPTH))
    val outputs = Range(0,n).map(_ => FIFO[rhs.B](OUTPUT_DEPTH))
    val impl = CircImpl(inputs, outputs)

    circImpls += lhs -> impl
  }

//  def getCircImpl[A:Bits,B:Bits](app: CircApply[A,B])(implicit ctx: SrcCtx) = {
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
//  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel { super.transform(lhs,rhs) }
    case _: BlackboxImpl[_,_,_] => inBox { super.transform(lhs,rhs) }

    case ctrl: Control[_] =>
      val stms = ctrl.bodies.flatMap(_.blocks).flatMap(_._2.stms)

      stms
        .filter(_.op.exists(_.isInstanceOf[CircNew[_,_]]))
        .foreach(s => createCircImpl(s.asInstanceOf[Circ[_,_]], s.op.get.asInstanceOf[CircNew[_,_]]))

      val appSyms = stms.filter(_.op.exists(_.isInstanceOf[CircApply[_,_]])).toSet
      if (appSyms.isEmpty) {
        return super.transform(lhs,rhs)
      }

      type TestA = argon.lang.Fix[TRUE,_32,_0]
      type TestB = argon.lang.Fix[TRUE,_32,_0]

      // Find the index of the first statement to use the result of a `CircApply` and the corresponding `CircApply`
      val (firstAppSym, firstIdx) = stms
        .map(_.inputs.find(appSyms(_)))
        .zipWithIndex
        .collectFirst{ case (Some(s), i) => (s, i) }
        .get

      val _firstApp =  firstAppSym.op.get.asInstanceOf[CircApply[_,_]]
//      implicit val evA: Bits[_firstApp.A] = _firstApp.evA
//      implicit val evB: Bits[_firstApp.B] = _firstApp.evB
      val firstApp = _firstApp.asInstanceOf[CircApply[TestA,TestB]]
      val impl = getCircImpl(firstApp)
      val (_left, _right) = stms.splitAt(firstIdx)
      val left = _left.filter(s => !appSyms(s) && !s.op.exists(_.isInstanceOf[CircNew[_,_]]))
      val right = _right.filter(s => !appSyms(s) && !s.op.exists(_.isInstanceOf[CircNew[_,_]]))

      val res = Stream {
        // Create enqueuing controller
        Pipe {
          isolateSubst() {
            left.foreach(visit)
            impl.inputs(firstApp.id).enq(firstApp.arg)
          }
        }

        // Create dequeuing controller
        Pipe {
          isolateSubst() {
            val res = impl.outputs(firstApp.id).deq()
            register(firstAppSym -> res)

            (appSyms - firstAppSym).foreach(visit)
            right.foreach(visit)
          }
        }
      }

      res.asInstanceOf[Sym[A]]

    case _ => super.transform(lhs,rhs)
  }
}

package spatial.transform

import argon._
import argon.tags.struct
import argon.lang.Bit
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.{mutable => mut}

// This pass removes everything `Circ` related from the IR by splitting up controllers containing `CircApply` nodes into
// pairs of controllers which enqueue to/dequeue from a controller which implements the behavior of the applied `Circ`.
// It must be run after `PipeInserter` because it assumes that `CircApply` results are scoped to an inner controller.

case class CircDesugaring(IR: State) extends MutateTransformer with AccelTraversal {
  private val INPUT_FIFO_DEPTH = 8;
  private val OUTPUT_FIFO_DEPTH = 8;

  type Tag = I32
  val Tag = I32
  type Tagged[A] = Tup2[Tag, A]

  def none[A:Bits]: Tagged[A] = Tup2(Tag(-1), Bits[A].zero)
  def some[A:Bits](t: Tag, d: A): Tagged[A] = Tup2(t, d)
  def tag[A:Bits](t: Tagged[A]): Tag = t._1
  def data[A:Bits](t: Tagged[A]): A = t._2

  case class CircImpl[A:Bits,B:Bits](
     kill: Reg[Bit],
     inputs: Seq[FIFO[Tagged[A]]],
     outputs: Seq[FIFO[B]],
  )

  private val circImpls: mut.Map[Circ[_,_],CircImpl[_,_]] = mut.HashMap.empty

  def declareCircImpl[A:Bits,B:Bits](lhs: Circ[A,B]): Unit = {
    val n = lhs.getNumApps
    val kill = Reg[Bit](Bit(false))
    val inputs = Range(0,n).map(_ => FIFO[Tagged[A]](INPUT_FIFO_DEPTH))
    val outputs = Range(0,n).map(_ => FIFO[B](OUTPUT_FIFO_DEPTH))

    val impl: CircImpl[A,B] = CircImpl(kill,inputs,outputs)
    circImpls += lhs -> impl
  }

  def getCircImpl[A:Bits,B:Bits](circ: Circ[A,B]): CircImpl[A,B] =
    circImpls(circ).asInstanceOf[CircImpl[A,B]]

  def declareProcessor[A:Bits,B:Bits](circ: Circ[A,B], circNew: CircNew[A,B]): Unit = {
    val impl = getCircImpl(circ)

    val count = Reg[Tag](0)
    Sequential(breakWhen = impl.kill).Foreach(*) { _ =>
      val input = priorityDeq(impl.inputs: _*)

      ifThenElse(tag(input) === -1,
        () => {
          count := count.value + Tag(1)
          impl.kill := count === circ.getNumApps
        },
        () => {
          val output = circNew.func(data(input))
          impl.outputs.zipWithIndex foreach {
            case (fifo, idx) =>
              val writeEnable = Tag(idx) === tag(input)
              fifo.enq(output, writeEnable)
          }
        }
      )
    }
  }

  def transformCtrl[A:Type](ctrl: Control[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = {
    val allStms = ctrl.bodies.flatMap(_.blocks).flatMap(_._2.stms)
    val newSyms = allStms.filter(_.op.exists(_.isInstanceOf[CircNew[_, _]])).toSet
    val appSyms = allStms.filter(_.op.exists(_.isInstanceOf[CircApply[_, _]])).toSet

    if (newSyms.isEmpty && appSyms.isEmpty) {
      return super.transform(ctrl.asInstanceOf[Sym[A]], rhs)
    }

    val result = Stream {
      for (s <- newSyms) {
        val erased: CircNew[_, _] = s.op.get.asInstanceOf[CircNew[_, _]]
        implicit val evA: Bits[erased.A] = erased.evA
        implicit val evB: Bits[erased.B] = erased.evB
        val circ: Circ[erased.A, erased.B] = s.asInstanceOf[Circ[erased.A, erased.B]]

        // EFFECTFUL: Stage allocation of circ structures
        declareCircImpl(circ)
      }

      for (s <- newSyms) {
        val erased: CircNew[_, _] = s.op.get.asInstanceOf[CircNew[_, _]]
        implicit val evA: Bits[erased.A] = erased.evA
        implicit val evB: Bits[erased.B] = erased.evB
        val circ: Circ[erased.A, erased.B] = s.asInstanceOf[Circ[erased.A, erased.B]]
        val circNew: CircNew[erased.A,erased.B] = erased.asInstanceOf[CircNew[erased.A,erased.B]]

        // EFFECTFUL: Stage processor
        declareProcessor(circ, circNew)
      }

      // Find the index of the first statement to use the result of a `CircApply` and the corresponding `CircApply`
      val readers: Seq[(Option[Sym[_]], Int)] = allStms.map(_.inputs.find(appSyms(_))).zipWithIndex
      val (appSym, readerIdx) = readers.collectFirst { case (Some(s), i) => (s, i) }.get

      val (left, right) = allStms.splitAt(readerIdx)
      val filteredLeft = left.filter(s => !appSyms(s) && !newSyms(s))
      val filteredRight = right.filter(s => !newSyms(s))
      val displacedAppSyms = (appSyms - appSym) -- right

      val erasedApp: CircApply[_, _] = appSym.op.get.asInstanceOf[CircApply[_, _]]
      implicit val evA: Bits[erasedApp.A] = erasedApp.evA
      implicit val evB: Bits[erasedApp.B] = erasedApp.evB
      val app: CircApply[erasedApp.A, erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A, erasedApp.B]]
      val impl = getCircImpl(app.circ)

      // EFFECTFUL: Stage enqueuing controller
      Pipe {
        isolateSubst() {
          filteredLeft.foreach(visit)
          impl.inputs(app.id).enq(some(app.id,app.arg))
          impl.inputs(app.id).enq(none)
        }
      }

      // EFFECTFUL: Stage dequeuing controller
      Pipe {
        isolateSubst() {
          val res = impl.outputs(app.id).deq()
          register(appSym -> res)

          displacedAppSyms.foreach(visit)
          filteredRight.foreach(visit)
        }
      }
    }

    result.asInstanceOf[Sym[A]]
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel { super.transform(lhs,rhs) }
    case _: BlackboxImpl[_,_,_] => inBox { super.transform(lhs,rhs) }
    case ctrl: Control[A] => transformCtrl(ctrl, rhs)
    case _ => super.transform(lhs,rhs)
  }
}

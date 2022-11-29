package spatial.transform

import argon._
import argon.lang.Bit
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal

import scala.collection.{mutable => mut}
import scala.collection.mutable.ArrayBuffer

// This pass removes everything `Circ` related from the IR by splitting up controllers containing `CircApply` nodes into
// pairs of controllers which enqueue to/dequeue from a controller which implements the behavior of the applied `Circ`.
// It must be run after `PipeInserter` because it assumes that `CircApply` results are scoped to an inner controller.

abstract class CircExecutorFactory {
  type Executor[A,B] <: CircExecutor[A,B]
  def stageExecutor[A:Bits,B:Bits](nApps: Int, func: A => B): Executor[A,B]
}

abstract class CircExecutor[A:Bits,B:Bits] {
  def stageDone(appId: Int): Void
  def stageEnq(appId: Int, data: A): Void
  def stageDeq(appId: Int): B
}

case class PriorityCircExecutorFactory(IR: State) extends CircExecutorFactory {
  final implicit def __IR: State = IR
  private val INPUT_FIFO_DEPTH = 8;
  private val OUTPUT_FIFO_DEPTH = 8;

  type Id = I32
  type Input[A] = Tup2[Id, A]

  val Id = I32
  def none[A:Bits]: Input[A] = Tup2(Id(-1), Bits[A].zero)
  def some[A:Bits](i: Id, a: A): Input[A] = Tup2(i, a)
  def id[A:Bits](i: Input[A]): Id = i._1
  def data[A:Bits](i: Input[A]): A = i._2

  case class PriorityCircExecutor[A:Bits,B:Bits](
    kill: Reg[Bit],
    inputs: Seq[FIFO[Input[A]]],
    outputs: Seq[FIFO[B]]
  ) extends CircExecutor[A, B] {
    override def stageDone(appId: Int): Void = inputs(appId).enq(none)
    override def stageEnq(appId: Int, data: A): Void = inputs(appId).enq(some(Id(appId), data))
    override def stageDeq(appId: Int): B = outputs(appId).deq()
  }

  override type Executor[A,B] = PriorityCircExecutor[A,B]

  override def stageExecutor[A:Bits,B:Bits](nApps: Int, func: A => B): Executor[A,B] = {
    val kill = Reg[Bit](Bit(false))
    val inputs = Range(0, nApps).map(_ => FIFO[Input[A]](INPUT_FIFO_DEPTH))
    val outputs = Range(0, nApps).map(_ => FIFO[B](OUTPUT_FIFO_DEPTH))
    val count = Reg[Id](0)
    val executor = PriorityCircExecutor(kill, inputs, outputs)

    Sequential(breakWhen = kill).Foreach(*) { _ =>
      val input = priorityDeq(inputs: _*)
      ifThenElse(id(input) === -1,
        () => {
          count := count.value + Id(1)
          kill := count === nApps
        },
        () => {
          val output = func(data(input))
          outputs.zipWithIndex foreach {
            case (fifo, idx) =>
              val writeEnable = Id(idx) === id(input)
              fifo.enq(output, writeEnable)
          }
        }
      )
    }

    executor
  }
}

// Type-safe wrapper around `mut.Map[Circ[_,_], CircExecutor[_,_]]`
case class ExecutorMap() {
  private val executors: mut.Map[Circ[_,_], CircExecutor[_,_]] = mut.HashMap.empty

  def +=[A:Bits,B:Bits](pair: (Circ[A,B], CircExecutor[A,B])): Unit =
    executors += pair

  def apply[A:Bits,B:Bits](circ: Circ[A,B]): CircExecutor[A,B] =
    executors(circ).asInstanceOf[CircExecutor[A,B]]
}

case class CircDesugaring(IR: State) extends MutateTransformer with AccelTraversal {
  private val factory: CircExecutorFactory = PriorityCircExecutorFactory(IR)
  private val executors: ExecutorMap = ExecutorMap()

  private def mirrorWithBody(ctrl: Control[_], producesResult: Boolean)(body: => Any): Void = ctrl match {
    case UnitPipe(ens,_,stopWhen) => stage(UnitPipe(Set.empty, stageBlock{ body; void }, None))
    case _ => throw new Exception("`CircApply` used in unsupported control context")
  }

  private def isNew(s: Sym[_]): Boolean = s.op.exists(_.isInstanceOf[CircNew[_,_]])
  private def isApp(s: Sym[_]): Boolean = s.op.exists(_.isInstanceOf[CircApply[_,_]])

  private def transformCtrlWithNewSyms(syms: Seq[Sym[_]], newSyms: mut.Set[Sym[_]]): Void = {
    Stream {
      for (s <- newSyms) {
        val erased: CircNew[_,_] = s.op.get.asInstanceOf[CircNew[_,_]]
        implicit val evA: Bits[erased.A] = erased.evA
        implicit val evB: Bits[erased.B] = erased.evB

        val circ: Circ[erased.A,erased.B] = s.asInstanceOf[Circ[erased.A,erased.B]]
        val circNew: CircNew[erased.A,erased.B] = erased.asInstanceOf[CircNew[erased.A, erased.B]]

        // EFFECTFUL!
        val executor = factory.stageExecutor(circ.getNumApps, circNew.func)
        executors += circ -> executor
      }

      Pipe {
        isolateSubst() {
          // EFFECTFUL!
          syms.filter(!isNew(_)) foreach visit
        }
      }
    }
  }

  private def transformCtrlWithAppSyms(syms: Seq[Sym[_]], appSyms: mut.Set[Sym[_]]): Void = {
    // We split off a new group everytime we see the first use of a `CircApply`
    val groups: ArrayBuffer[(ArrayBuffer[Sym[_]], mut.Set[Sym[_]], mut.Set[Sym[_]])] =
      ArrayBuffer((ArrayBuffer(), mut.Set(), mut.Set()))

    // CAUTION: We destroy `appSyms` as we iterate
    for (s <- syms) {
      val appSymInputs = s.inputs.filter(appSyms.contains)
      if (appSymInputs.nonEmpty) {
        groups += ((ArrayBuffer(), mut.Set(), appSymInputs.to[mut.Set]))
        appSyms --= appSymInputs
      }

      groups.last._1 += s
      if (isApp(s)) {
        groups.last._2 += s
      }
    }

    Stream {
      isolateSubst() {
        for ((groupSyms, groupEnqs, groupDeqs) <- groups) {
          Pipe {
            for (appSym <- groupDeqs) {
              val erasedApp: CircApply[_,_] = appSym.op.get.asInstanceOf[CircApply[_,_]]
              implicit val evA: Bits[erasedApp.A] = erasedApp.evA
              implicit val evB: Bits[erasedApp.B] = erasedApp.evB

              val app: CircApply[erasedApp.A,erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A,erasedApp.B]]
              val executor = executors(app.circ)

              // EFFECTFUL!
              val output = executor.stageDeq(app.id)
              register(appSym -> output)
            }

            // EFFECTFUL: Visiting a `CircApply` will replace it via `executor.stageEnq` (see `transformApp`)
            groupSyms.foreach(visit)

            for (appSym <- groupEnqs) {
              val erasedApp: CircApply[_,_] = appSym.op.get.asInstanceOf[CircApply[_,_]]
              implicit val evA: Bits[erasedApp.A] = erasedApp.evA
              implicit val evB: Bits[erasedApp.B] = erasedApp.evB

              val app: CircApply[erasedApp.A,erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A,erasedApp.B]]
              val executor = executors(app.circ)

              // EFFECTFUL!
              executor.stageDone(app.id)
            }
          }
        }
      }
    }
  }

  private def transformCtrl[A:Type](lhs: Sym[A], ctrl: Control[A])(implicit ctx: SrcCtx): Sym[A] = {
    val syms = ctrl.bodies.flatMap(_.blocks).flatMap(_._2.stms)
    val newSyms: mut.Set[Sym[_]] = syms.filter(isNew).to[mut.Set]
    val appSyms: mut.Set[Sym[_]] = syms.filter(isApp).to[mut.Set]

    if (newSyms.isEmpty && appSyms.isEmpty) {
      return super.transform(lhs, ctrl)
    }

    val result = if (newSyms.nonEmpty) {
      // `CircApply` is `Primitive` but `CircNew` is not, so the two never coexist
      assert(appSyms.isEmpty)
      transformCtrlWithNewSyms(syms, newSyms)
    } else {
      assert(appSyms.nonEmpty)
      transformCtrlWithAppSyms(syms, appSyms)
    }

    result.asInstanceOf[Sym[A]]
  }

  private def transformApp(erasedApp: CircApply[_,_])(implicit ctx: SrcCtx): Sym[_] = {
    implicit val evA: Bits[erasedApp.A] = erasedApp.evA
    implicit val evB: Bits[erasedApp.B] = erasedApp.evB
    val app: CircApply[erasedApp.A, erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A, erasedApp.B]]

    // We create an executor when we see `CircNew`, so the right executor must exist by the time we see the
    // corresponding `CircApply`
    val executor = executors(app.circ)
    executor.stageEnq(app.id,app.arg)
  }

  override def transform[A:Type](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx): Sym[A] = rhs match {
    case AccelScope(_) => inAccel { super.transform(lhs,rhs) }
    case _: BlackboxImpl[_,_,_] => inBox { super.transform(lhs,rhs) }
    case ctrl: Control[A] => transformCtrl(lhs, ctrl)
    case app: CircApply[_,_] => transformApp(app).asInstanceOf[Sym[A]]
    case _ => super.transform(lhs,rhs)
  }
}

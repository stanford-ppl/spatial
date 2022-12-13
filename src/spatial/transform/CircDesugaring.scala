package spatial.transform

import argon._
import argon.lang.Bit
import argon.node._
import argon.transform.MutateTransformer
import spatial.lang._
import spatial.node._
import spatial.traversal.AccelTraversal
import spatial.metadata.types._
import spatial.metadata.memory._

import scala.collection.immutable.{Stream => ScalaStream}
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
  private val INPUT_FIFO_DEPTH = 8
  private val OUTPUT_FIFO_DEPTH = 8

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
      val output = func(data(input))
      outputs.zipWithIndex foreach {
        case (fifo, idx) =>
          val writeEnable = Id(idx) === id(input)
          fifo.enq(output, writeEnable)
      }
      retimeGate()
      val newCount = count.value + mux(id(input) === Id(-1), Id(1), Id(0))
      count.write(newCount)
      kill.write(true, newCount === Id(nApps))
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

  private def isNew(s: Sym[_]): Boolean = s.op.exists(_.isInstanceOf[CircNew[_,_]])
  private def isApp(s: Sym[_]): Boolean = s.op.exists(_.isInstanceOf[CircApply[_,_]])

  // https://github.com/scala/bug/issues/12463
  private def shouldLift(s: Sym[_]): Boolean = s.isReg || s.isRegFile || s.isFIFO || s.isLIFO || s.isLUT

  private def stageExecutors(newSyms: mut.Set[Sym[_]]): Void = {
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
  }

  case class Group(
    syms: ArrayBuffer[Sym[_]],
    execEnqs: mut.Set[Sym[_]],
    execDeqs: mut.Set[Sym[_]],
    sendVars: mut.Map[Sym[_] /* var */, Int /* receiver */],
    recvVars: mut.Set[Sym[_]],
  )

  private def stageSymsInGroups(syms: Seq[Sym[_]], appSyms: mut.Set[Sym[_]]): Void = {
    val groups = ArrayBuffer(Group(ArrayBuffer(), mut.Set(), mut.Set(), mut.Map(), mut.Set()))
    val liftedSyms: ArrayBuffer[Sym[_]] = syms.filter(shouldLift).to[ArrayBuffer]

    // Map from `Sym`s seen so far to index of last group where each `Sym` appeared; updated during loop below
    val prevSyms: mut.Map[Sym[_], Int] = mut.Map()
    var groupIdx: Int = 0
    val unusedAppSyms = appSyms.clone

    // We ignore `CircNew` nodes when constructing groups; they are handled by `stageExecutors` in `transformCtrl`
    for (s <- syms.filter(s => !isNew(s) && !shouldLift(s))) {
      val execDeqs = s.inputs.filter(unusedAppSyms.contains)

      // Split off a new group everytime we see the first use of a `CircApply`
      if (execDeqs.nonEmpty) {
        unusedAppSyms --= execDeqs
        val group = groups.last

        // We will need to forward each variable that was forwarded to us to the next user
        for (s <- group.recvVars) {
          prevSyms(s) = groupIdx
        }
        for (s <- group.execDeqs) {
          prevSyms(s) = groupIdx
        }
        // We will also need to forward any variables created in our scope
        prevSyms ++= group.syms.zip(ScalaStream.continually(groupIdx))

        groups += Group(ArrayBuffer(), mut.Set(), execDeqs.to[mut.Set], mut.Map(), mut.Set())
        groupIdx += 1
      }

      val group = groups.last
      group.syms += s

      if (isApp(s)) {
        group.execEnqs += s
      }

      val recvVars = s.inputs.filter(s => prevSyms.contains(s) && !group.execDeqs.contains(s))
      group.recvVars ++= recvVars
      for (s <- recvVars) {
        val sender = groups(prevSyms(s))
        assert(!sender.sendVars.contains(s))
        sender.sendVars += s -> groupIdx
      }
    }

    dbgs(s"Lifting: $liftedSyms")
    dbgs(s"Groups: $groups")

    isolateSubst() {
      // EFFECTFUL!
      liftedSyms.foreach(visit)

      val sendVarFifos: ArrayBuffer[Map[Sym[_], FIFO[_]]] = ArrayBuffer.fill(groups.length)(Map.empty)
      val recvVarFifos: ArrayBuffer[Map[Sym[_], FIFO[_]]] = ArrayBuffer.fill(groups.length)(Map.empty)

      // Create FIFOs for intermedates
      for ((group, i) <- groups.zipWithIndex) {
        for ((s, j) <- group.sendVars) {
          if (!s.op.exists(_.R.isBits)) {
            throw new Exception(
              """Only memory allocations and types satisfying `Bits` may be created in the same scope as a `Circ`
                |application and referenced after any reference to the result of that application""".stripMargin
            )
          }

          implicit val sBits: Bits[s.MySpecialType] = s.asInstanceOf[Bits[s.MySpecialType]]

          // EFFECTFUL!
          val fifo = FIFO[s.MySpecialType](1)
          sendVarFifos(i) += s -> fifo
          assert(groups(j).recvVars.contains(s))
          recvVarFifos(j) += s -> fifo
        }
      }

      for ((Group(groupSyms, execEnqs, execDeqs, sendVars, recvVars), i) <- groups.zipWithIndex) {
        Pipe {
          for (appSym <- execDeqs) {
            val erasedApp: CircApply[_,_] = appSym.op.get.asInstanceOf[CircApply[_,_]]
            implicit val evA: Bits[erasedApp.A] = erasedApp.evA
            implicit val evB: Bits[erasedApp.B] = erasedApp.evB

            val app: CircApply[erasedApp.A,erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A,erasedApp.B]]
            val executor = executors(app.circ)

            // EFFECTFUL!
            val output = executor.stageDeq(app.id)
            register(appSym -> output)
          }

          for (s <- recvVars) {
            // EFFECTFUL!
            val output = recvVarFifos(i)(s).deq()
            register(s -> output)
          }

          // EFFECTFUL: Visiting a `CircApply` will replace it with an `executor.stageEnq` (see `transformApp`)
          groupSyms.foreach(visit)

          for ((s, _) <- sendVars) {
            implicit val sBits: Bits[s.MySpecialType] = s.asInstanceOf[Bits[s.MySpecialType]]
            val fifo: FIFO[s.MySpecialType] = sendVarFifos(i)(s).asInstanceOf[FIFO[s.MySpecialType]]

            // EFFECTFUL!
            fifo.enq(f(s).asInstanceOf[s.MySpecialType])
          }

          for (appSym <- execEnqs) {
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

  private def transformCtrl[A:Type](lhs: Sym[A], ctrl: Control[A])(implicit ctx: SrcCtx): Sym[A] = {
    val syms = ctrl.bodies.flatMap(_.blocks).flatMap(_._2.stms)
    val newSyms: mut.Set[Sym[_]] = syms.filter(isNew).to[mut.Set]
    val appSyms: mut.Set[Sym[_]] = syms.filter(isApp).to[mut.Set]

    if (newSyms.isEmpty && appSyms.isEmpty) {
      return super.transform(lhs, ctrl)
    }

    dbgs(s"In ctrl $lhs:")
    dbgs(s"\tFound `CircNew`s: $newSyms")
    dbgs(s"\tFound `CircApp`s: $appSyms")

    val result = Stream {
      stageExecutors(newSyms)
      stageSymsInGroups(syms, appSyms)
    }

    result.asInstanceOf[Sym[A]]
  }

  private def transformApp(erasedApp: CircApply[_,_])(implicit ctx: SrcCtx): Sym[_] = {
    implicit val evA: Bits[erasedApp.A] = erasedApp.evA
    implicit val evB: Bits[erasedApp.B] = erasedApp.evB
    val app: CircApply[erasedApp.A, erasedApp.B] = erasedApp.asInstanceOf[CircApply[erasedApp.A, erasedApp.B]]

    // We create an executor when we see a `CircNew`, so the right executor must exist by the time we see the
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

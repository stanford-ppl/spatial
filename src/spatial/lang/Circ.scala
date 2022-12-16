package spatial.lang

import argon._
import forge.tags._
import spatial.node._

@ref class Circ[A:Bits,B:Bits] extends Ref[Any,Circ[A,B]] {
  override protected val __neverMutable: Boolean = true

  private var numApps = 0
  def getNumApps: Int = numApps

  @api def apply(x: A): B = {
    val id = numApps
    numApps += 1
    stage(CircApply(this,id,x))
  }

  // TODO: we need access to the underlying function
  // @api def func: A => B = ...
  // @api def duplicate(): Circ[A,B] = Circ(func())
}

object Circ {
  @api def apply[A:Bits,B:Bits](func: A => B, factory: CircExecutorFactory[A,B]): Circ[A,B] = {
    stage(CircNew(func, factory))
  }

  @api def apply[A:Bits,B:Bits](func: A => B): Circ[A,B] = {
    Circ(func, PriorityCircExecutorFactory(8, 8))
  }
}

abstract class CircExecutorFactory[A:Bits,B:Bits] {
  type Executor <: CircExecutor[A,B]
  def stageExecutor(nApps: Int, func: A => B): Executor
}

abstract class CircExecutor[A:Bits,B:Bits] {
  def stageDone(appId: Int): Void
  def stageEnq(appId: Int, data: A): Void
  def stageDeq(appId: Int): B
}

case class PriorityCircExecutorFactory[A:Bits,B:Bits](
  input_fifo_depth: Int,
  output_fifo_depth: Int,
)(
  implicit IR: State
) extends CircExecutorFactory[A,B] with Mirrorable[PriorityCircExecutorFactory[A,B]] {
  override def mirror(f: Tx): PriorityCircExecutorFactory[A,B] = this

  type Id = I32
  type Input = Tup2[Id, A]

  private val Id = I32
  private def none: Input = Tup2(Id(-1), Bits[A].zero)
  private def some(i: Id, a: A): Input = Tup2(i, a)
  private def id(i: Input): Id = i._1
  private def data(i: Input): A = i._2

  case class PriorityCircExecutor(
    kill: Reg[Bit],
    inputs: Seq[FIFO[Input]],
    outputs: Seq[FIFO[B]],
  ) extends CircExecutor[A,B] {
    override def stageDone(appId: Int): Void = inputs(appId).enq(none)
    override def stageEnq(appId: Int, data: A): Void = inputs(appId).enq(some(Id(appId), data))
    override def stageDeq(appId: Int): B = outputs(appId).deq()
  }

  override type Executor = PriorityCircExecutor

  override def stageExecutor(nApps: Int, func: A => B): Executor = {
    val kill = Reg[Bit](Bit(false))
    val inputs = Range(0, nApps).map(_ => FIFO[Input](input_fifo_depth))
    val outputs = Range(0, nApps).map(_ => FIFO[B](output_fifo_depth))
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

package spatial.executor.scala.memories

import spatial.executor.scala.{EmulMem, EmulResult, SimulationException}

import scala.collection.mutable

class ScalaQueue[T <: EmulResult](val capacity: Int = scala.Int.MaxValue) extends EmulMem {
  val queue = new mutable.Queue[T]()

  def isFull: Boolean = queue.size >= capacity
  def isEmpty: Boolean = queue.isEmpty

  def enq(value: T): Unit = {
    if (isFull) {
      throw SimulationException(s"Attempting to enqueue to a full FIFO")
    }
    queue.enqueue(value)
  }

  def deq(): T = {
    if (isEmpty) {
      throw  SimulationException(s"Attempting to dequeue from an empty FIFO")
    }
    queue.dequeue()
  }

  def head: T = queue.head
  def headOption: Option[T] = queue.headOption
  def size: Int = queue.size

  override def toString: String = {
    s"Queue(capacity = $capacity, size = $size)"
  }
}

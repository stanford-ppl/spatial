package spatial.executor.scala.memories

import argon.ExpType
import spatial.executor.scala.{EmulMem, EmulResult, EmulVal, ExecUtils, SimulationException, SomeEmul}

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

class FileReadScalaQueue(filename: String, fmt: ExpType[_, _]) extends ScalaQueue[SomeEmul] {
  private val file = scala.io.Source.fromFile(filename)
  file.getLines().foreach {
    line => queue.enqueue(ExecUtils.parse(line.split(",").toIterator, fmt))
  }
  file.close()

  override def enq(value: SomeEmul): Unit = {
    throw SimulationException(s"Cannot Enqueue to a FileRead Stream!")
  }
}

class FileWriteScalaQueue(filename: String) extends ScalaQueue[EmulVal[_]] {
  private val file = new java.io.FileWriter(filename)

  override def deq(): EmulVal[_] = {
    throw SimulationException(s"Cannot Dequeue from a FileWrite Stream!")
  }
  override def enq(value: EmulVal[_]): Unit = {
    val serialized = ExecUtils.serialize(value)
    file.write(serialized.mkString("", ", ", "\n"))
  }
}



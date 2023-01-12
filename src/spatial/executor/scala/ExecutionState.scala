package spatial.executor.scala

import argon.{Const, Exp, Param, Sym, Value, dbgs, stm}
import emul.Bool
import spatial.executor.scala.ExecutionState.getNewID
import spatial.executor.scala.memories.ScalaTensor
import spatial.executor.scala.resolvers.OpResolver

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

// Tracks the state of addresses to tensors on the host side
case class MemEntry(start: Int, size: Int, tensor: ScalaTensor[SomeEmul])
class MemTracker {
  var mems: ArrayBuffer[MemEntry] = ArrayBuffer.empty
  var maxAddress = 0
  def register[T <: EmulResult](tensor: ScalaTensor[T]): Unit = {
    // "Allocate" a block of memory
    val blockSize = tensor.size * tensor.elementSize.get
    mems.append(MemEntry(maxAddress, blockSize, tensor.asInstanceOf[ScalaTensor[SomeEmul]]))
    maxAddress += blockSize
  }

  def getEntry[T <: EmulResult](tensor: ScalaTensor[T]): MemEntry = {
    mems.find(_.tensor == tensor).get
  }

  override def toString: String = {
    s"MemTracker: ${mems.mkString(", ")}"
  }
}

object ExecutionState {
  var nextID: Int = 0

  def getNewID(): Int = {
    nextID += 1
    nextID - 1
  }
}
class ExecutionState(var values: Map[Exp[_, _], EmulResult], val hostMem: MemTracker, val memoryController: MemoryController, implicit val IR: argon.State) {
  val ID: Int = getNewID()
  def apply[U, V](s: Exp[U, V]): EmulResult = s match {
    case Value(result: Boolean) => SimpleEmulVal(emul.Bool(result))
    case Value(result) =>
      SimpleEmulVal(result)
    case _ => values(s)
  }

  def getValue[T](s: Exp[_, _]): T = {
    val recv = this(s)
    recv match {
      case ev: EmulVal[T] if ev.valid => ev.value
      case ev: EmulVal[T] if !ev.valid =>
        throw new Exception(s"$s -> $ev was marked invalid!")
    }
  }

  def getTensor[T <: EmulResult](s: Exp[_, _]): ScalaTensor[T] = {
    this(s) match {
      case st: ScalaTensor[T] => st
    }
  }

  def register(sym: Sym[_], v: EmulResult): Unit = {
    dbgs(s"Registering: ${stm(sym)} -> $v")
    values += (sym -> v)
  }

  def copy(): ExecutionState = new ExecutionState(values, hostMem, memoryController, IR)

  def runAndRegister[U, V](s: Exp[U, V]): EmulResult = {
    val result = OpResolver.run(s, this)
    register(s, result)
    result
  }

  override def toString: String = {
    s"ExecutionState($ID)"
  }
}

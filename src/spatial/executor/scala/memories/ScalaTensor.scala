package spatial.executor.scala.memories

import argon.{Op, Sym}
import spatial.executor.scala.{EmulMem, EmulResult, EmulVal, SimulationException}
import spatial.node.MemAlloc

import scala.reflect.ClassTag


// Shape is from largest stride to smallest stride
// Elementsize is needed to perform DRAM/SRAM transfers, and is measured in bytes
class ScalaTensor[T <: EmulResult](val shape: Seq[Int], val elementSize: Option[Int], initValues: Option[Seq[Option[T]]] = None) extends EmulMem {
  type ET = T

  val size = shape.product
  val strides = shape.scanRight(1)(_*_).drop(1)
  val values: Array[Option[T]] = initValues match {
    case None => Array.fill[Option[T]](size)(None)
    case Some(initVals) =>
      if (initVals.size != size) {
        throw new IllegalArgumentException(s"Attempting to initialize a tensor of shape $shape [$size] with ${initVals.size} elements: $initVals")
      }
      initVals.toArray
  }

  def flattenIndex(index: Seq[Int]): Int = {
    if (index.size != shape.size) {
      throw new IllegalArgumentException(s"Expected an index of size ${shape.size} but received ${index.size} instead")
    }
    val isOOB = (index zip shape).exists { case (ind, ub) => ind < 0 || ind >= ub }
    if (isOOB) {
      throw new IndexOutOfBoundsException(s"Index $index was out of bounds, shape was $shape")
    }

    (index zip strides).map {case (a, b) => a * b }.sum
  }

  def write(data: T, address: Seq[Int], en: Boolean): Unit = if (en) {
    values(flattenIndex(address)) = Some(data)
  }

  def read(address: Seq[Int], en: Boolean): Option[T] = {
    if (!en) {
      None
    } else {
      values(flattenIndex(address))
    }
  }
  def reset(): Unit = {
    initValues match {
      case None => throw SimulationException("Cannot reset a memory with no initializer!")
      case Some(rvals) =>
        rvals.zipWithIndex.foreach {
          case (value, ind) =>
            values(ind) = value
        }
    }
  }
  override def toString: String = s"ScalaTensor(shape = ${shape}, elementSize = $elementSize, size = $size, strides = $strides, values = <${values.map(_.toString).mkString(", ")}>)"
}

class ScalaLB[T <: EmulResult](shape: Seq[Int], elementSize: Option[Int], initValues: Option[Seq[Option[T]]] = None) extends ScalaTensor[T](shape, elementSize, initValues) {
  var nextAddr: Int = 0
}

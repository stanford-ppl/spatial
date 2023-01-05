package spatial.executor.scala.memories

import argon.{Op, Sym}
import spatial.executor.scala.{EmulMem, EmulVal}
import spatial.node.MemAlloc

import scala.reflect.ClassTag


// Shape is from largest stride to smallest stride
class ScalaTensor[TP: ClassTag](val shape: Seq[Int], initValues: Option[Seq[Option[TP]]] = None) extends EmulMem[TP] {
  type ET = TP

  val size = shape.product
  val strides = shape.scanRight(1)(_*_).drop(1)
  val values = initValues match {
    case None => Array.fill[Option[TP]](size)(None)
    case Some(initVals) =>
      if (initVals.size != size) {
        throw new IllegalArgumentException(s"Attempting to initialize a tensor of shape $shape [$size] with ${initVals.size} elements: $initVals")
      }
      initVals.toArray
  }

  private def flattenIndex(index: Seq[Int]): Int = {
    if (index.size != shape.size) {
      throw new IllegalArgumentException(s"Expected an index of size ${shape.size} but received ${index.size} instead")
    }
    val isOOB = (index zip shape).exists { case (ind, ub) => ind < 0 || ind >= ub }
    if (isOOB) {
      throw new IndexOutOfBoundsException(s"Index $index was out of bounds, shape was $shape")
    }

    (index zip strides).map {case (a, b) => a * b }.product
  }

  def write(data: TP, address: Seq[Int], en: Boolean): Unit = if (en) {
    values(flattenIndex(address)) = Some(data)
  }

  def read(address: Seq[Int], en: Boolean): Option[TP] = {
    if (!en) {
      None
    } else {
      values(flattenIndex(address))
    }
  }

  override lazy val tag: ClassTag[TP] = implicitly[ClassTag[TP]]
}
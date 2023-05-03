package spatial.tests.ee109

import spatial.dsl._

import scala.util.Random

object Utils {
  val maxInt = 64
  val r = new Random(1)

  def createArray(nItems: scala.Int, maxSize: scala.Int = maxInt): scala.Array[scala.Int] =
    scala.Array.tabulate[scala.Int](nItems) { _ =>
      r.nextInt(maxSize) + 1
    }

  def toSpatialIntArray(array: scala.Array[scala.Int])(
    implicit state: argon.State): Array[Int] =
    Array.fromSeq[Int](array.toSeq.map(m => m.to[Int]))
}


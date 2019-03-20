package spatial.dse

import java.util.concurrent.LinkedBlockingQueue

import argon.State
import spatial.metadata.params._
import spatial.metadata.bounds._

import scala.collection.mutable.ArrayBuffer

case class PruneWorker(
  start: Int,
  size: Int,
  prods: Seq[BigInt],
  dims:  Seq[BigInt],
  indexedSpace: Seq[(Domain[_],Int)],
  restricts: Set[Restrict],
  queue: LinkedBlockingQueue[Seq[Int]]
)(implicit state: State) extends Runnable {

  private def isLegalSpace(): Boolean = restricts.forall(_.evaluate())

  def run(): Unit = {
    println(s"Searching from $start until ${start+size}")
    val pts = (start until (start+size)).filter{i =>
      indexedSpace.foreach{case (domain,d) => domain.set( ((i / prods(d)) % dims(d)).toInt ) }
      isLegalSpace()
    }
    queue.put(pts)
  }
}
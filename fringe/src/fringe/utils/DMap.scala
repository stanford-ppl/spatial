package fringe.utils

import fringe.utils.Banks._
import scala.collection.immutable.ListMap

object DMap {
  /* Map from muxPort to (Banks, isShift) */
  type DMap = ListMap[(Int,Int,Int), (List[Banks],Option[Int])]
  implicit class DMapOps(x: DMap) {
    def muxAddrs: Seq[(Int,Int,Int)] = x.keys.toSeq
    def accessPars: Seq[Int] = x.sortByMuxPortAndOfs.values.map(_._1.length).toSeq
    def shiftAxis: Option[Int] = x.values.head._2
    def sortByMuxPortAndOfs: DMap = DMap(x.toSeq.sortBy(r => (r._1._1, r._1._2, r._1._3)))
    def sortByMuxPortAndCombine: DMap = DMap(x.toSeq.groupBy(_._1._1).map{case (muxP, entries) => (muxP, 0, 0) -> (entries.sortBy(x => (x._1._1, x._1._2, x._1._3)).flatMap(_._2._1).toList, entries.head._2._2)}.toSeq.sortBy(r => (r._1._1, r._1._2, r._1._3))) // Combine entries so that every muxOfs = 0, then sort
    def accessParsBelowMuxPort(mport: Int, mofs: Int, castgrp: Int): Seq[Int] = x.sortByMuxPortAndOfs.filter{p => p._1._1 < mport | (p._1._1 == mport & p._1._2 < mofs) | (p._1._1 == mport & p._1._2 == mofs & p._1._3 < castgrp)}.accessPars
  }

  def apply(xs:((Int,Int,Int),(List[Banks], Option[Int]))*): DMap = ListMap[(Int,Int,Int), (List[Banks],Option[Int])](xs.map{x => x._1 -> x._2}:_*)

  // Example: val b = DMap((0,0) -> List(Banks(0,0), Banks(0,1)), (0,2) -> List(Banks(0,2),Banks(0,3)), (1,0) -> List(Banks(0,0),Banks(1,0)))
  def apply(xs: => Seq[((Int,Int,Int), (List[Banks],Option[Int]))]): DMap = ListMap[(Int,Int,Int),(List[Banks],Option[Int])](xs.map{case(k,v) => k -> v}:_*)

}

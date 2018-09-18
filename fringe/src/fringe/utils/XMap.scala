package fringe.utils

import scala.collection.immutable.ListMap

object XMap {
  /* Map from (muxPort, muxOfs, castgroup) to (width of muxPort, isShift) */
  type XMap = ListMap[(Int, Int, Int), (Int, Option[Int])]
  implicit class XMapOps(x: XMap) {
    def muxAddrs: Seq[(Int,Int,Int)] = x.keys.toSeq
    def accessPars: Seq[Int] = x.sortByMuxPortAndOfs.values.map(_._1).toSeq
    def shiftAxis: Option[Int] = x.values.head._2
    def sortByMuxPortAndOfs: XMap = XMap(x.toSeq.sortBy(r => (r._1._1, r._1._2, r._1._2))) // Order the map by (muxPort, muxOfs)
    def sortByMuxPortAndCombine: XMap = XMap(x.toSeq.groupBy(_._1._1).map{case (muxP, entries) => (muxP, 0, 0) -> (entries.sortBy(x => (x._1._1, x._1._2, x._1._3)).map(_._2._1).sum, entries.head._2._2)}.toSeq.sortBy(r => (r._1._1, r._1._2, r._1._3))) // Combine entries so that every muxOfs = 0, then sort
    def accessParsBelowMuxPort(mport: Int, mofs: Int, castgrp: Int): Seq[Int] = x.sortByMuxPortAndOfs.filter{p => p._1._1 < mport | (p._1._1 == mport & p._1._2 < mofs) | (p._1._1 == mport & p._1._2 == mofs & p._1._3 < castgrp)}.accessPars
    def merge(y: XMap): XMap = {
      if (y.nonEmpty) {
        ListMap( (x ++ ListMap(y.map{case (k,v) =>
          val base = x.toList.length
          ({base + k._1}, 0, 0) -> v
        }.toArray:_*)).toArray:_*)
      } else x
    }
  }
  def XMap(xs:((Int, Int, Int), (Int, Option[Int]))*): XMap = ListMap[(Int,Int,Int),(Int,Option[Int])](xs.map{x => x._1 -> x._2}:_*)
  // Example: val a = XMap((0,0) -> 2, (0,2) -> 3, (1,0) -> 4)
  def XMap(xs: => Seq[((Int,Int,Int), (Int,Option[Int]))]): XMap = ListMap[(Int,Int,Int),(Int,Option[Int])](xs.map{case(k,v) => k -> v}:_*)

}

package fringe.utils

import fringe.utils.DMap._
import scala.collection.immutable.ListMap

object NBufDMap {

  type NBufDMap = ListMap[Int, DMap]
  def apply(xs:(Int,DMap)*): NBufDMap = ListMap[Int, DMap](xs:_*)
  def apply(xs: => Seq[(Int, DMap)]): NBufDMap = ListMap[Int,DMap](xs:_*)

  implicit class NBufDMapOps(x: NBufDMap) {
    def mergeDMaps: DMap = {
      ListMap(x.sortByBufferPort.map{case (buf,map) =>
        val base = x.filter(_._1 < buf).values.toList.flatten.map(_._1).length
        map.map{case (muxport, banks) => ({muxport._1 + base}, muxport._2, muxport._3) -> banks }
      }.flatten.toArray:_*)
    }
    def accessPars: Seq[Int] = x.mergeDMaps.accessPars
    def accessParsBelowBufferPort(f: Int): Seq[Int] = x.sortByBufferPort.filter(_._1 < f).mergeDMaps.accessPars
    def sortByBufferPort: NBufDMap = NBufDMap(x.toSeq.sortBy(_._1))
  }

}

package fringe.utils

import fringe.utils.XMap._
import scala.collection.immutable.ListMap

object NBufXMap {

  type NBufXMap = ListMap[Int, XMap]
  def apply(xs:(Int, XMap)*): NBufXMap = ListMap[Int,XMap](xs:_*)
  def apply(xs: => Seq[(Int, XMap)]): NBufXMap = ListMap[Int,XMap](xs:_*)
  implicit class NBufXMapOps(x: NBufXMap) {
    def mergeXMaps: XMap = {
      ListMap(x.sortByBufferPort.map{case (buf,map) =>
        val base = x.filter(_._1 < buf).values.toList.flatten.map(_._1).length
        map.map{case (muxport, par) => ({muxport._1 + base}, muxport._2, muxport._3) -> par }
      }.flatten.toArray:_*)
    }
    def accessPars: Seq[Int] = x.mergeXMaps.accessPars
    def accessParsBelowBufferPort(f: Int): Seq[Int] = x.sortByBufferPort.filter(_._1 < f).mergeXMaps.accessPars
    def sortByBufferPort: NBufXMap = NBufXMap(x.toSeq.sortBy(_._1))
  }

}


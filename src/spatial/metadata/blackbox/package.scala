package spatial.metadata

import argon._
import spatial.node._

package object blackbox {

  implicit class BlackboxOps(s: Sym[_]) {
    def getBboxInfo: Option[BlackboxConfig] = metadata[BlackboxInfo](s).map(_.cfg)
    def bboxInfo: BlackboxConfig = getBboxInfo.getOrElse(BlackboxConfig(""))
    def bboxInfo_=(cfg: BlackboxConfig): Unit = metadata.add(s, BlackboxInfo(cfg))
    def bboxII: Double = if (getBboxInfo.isDefined && !bboxInfo.pipelined) bboxInfo.latency else 1.0
  }
}

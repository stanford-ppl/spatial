package spatial.metadata

import argon._
import spatial.node._
import spatial.metadata.control._

package object blackbox {

  implicit class BlackboxOps(s: Sym[_]) {
    def getBboxInfo: Option[BlackboxConfig] = metadata[BlackboxInfo](s).map(_.cfg)
    def bboxInfo: BlackboxConfig = getBboxInfo.getOrElse(BlackboxConfig(""))
    def bboxInfo_=(cfg: BlackboxConfig): Unit = metadata.add(s, BlackboxInfo(cfg))
    def bboxII: Double = if (getBboxInfo.isDefined) bboxInfo.pf else if (isSpatialPrimitiveBlackbox) s.II else 1.0

    def getUserNodes: Option[Seq[Sym[_]]] = metadata[BlackboxUserNodes](s).map(_.node)
    def userNodes: Seq[Sym[_]] = getUserNodes.getOrElse(Seq[Sym[_]]())
    def addUserNode(node: Sym[_]): Unit = metadata.add(s, BlackboxUserNodes(userNodes :+ node))

    def isCtrlBlackbox: Boolean = s.op.exists{ case _: VerilogCtrlBlackbox[_,_] => true; case _: SpatialCtrlBlackboxUse[_,_] => true; case _: SpatialCtrlBlackboxImpl[_,_] => true; case _ => false}
    def isBlackboxImpl: Boolean = s.op.exists{ case _: BlackboxImpl[_,_,_] => true; case _ => false}
    def isBlackboxUse: Boolean = s.op.exists{ case _: CtrlBlackboxUse[_,_] => true; case _ => false}
    def isSpatialPrimitiveBlackbox: Boolean = s.op.exists{ case _: SpatialBlackboxImpl[_,_] => true; case _ => false}
  }
}

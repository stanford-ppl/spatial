package spatial.metadata

import argon.{Sym, metadata}

import spatial.metadata.control._

package object transform {
  implicit class TransformOps(s: Sym[_]) {
    def isStreamPrimitive: Boolean = metadata[StreamPrimitive](s).exists(_.flag)
    def isStreamPrimitive_=(flag: Boolean): Unit = metadata.add[StreamPrimitive](s, StreamPrimitive(flag))
    def hasStreamPrimitiveAncestor: Boolean = s.toCtrl.hasStreamPrimitiveAncestor

    def freezeMem: Boolean = metadata[FreezeMem](s).exists(_.flag)
    def freezeMem_=(flag: Boolean): Unit = metadata.add[FreezeMem](s, FreezeMem(flag))
  }

  implicit class TransformCtrlOps(s: Ctrl) {
    def getStreamPrimitiveAncestor: Option[Ctrl] = {
      s.ancestors.find {
        case Ctrl.Node(c, _) => c.isStreamPrimitive
        case _ => false
      }
    }

    def isStreamPrimitive: Boolean = s match {
      case Ctrl.Node(s, _) => s.isStreamPrimitive
      case _ => false
    }

    def hasStreamPrimitiveAncestor: Boolean = getStreamPrimitiveAncestor.nonEmpty
  }
}

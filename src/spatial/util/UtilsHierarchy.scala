package spatial.util

import argon._
import spatial.data._
import spatial.node._

trait UtilsHierarchy {

  implicit class OpHierarchy(op: Op[_]) {
    def isControl: Boolean = op.isInstanceOf[Control[_]]
    def isPrimitive: Boolean = op.isInstanceOf[Primitive[_]]
    def isEphemeral: Boolean = op match {
      case p: Primitive[_] => p.isEphemeral
      case _ => false
    }

    def isAccel: Boolean = op.isInstanceOf[AccelScope]

    def isSwitch: Boolean = op.isInstanceOf[Switch[_]]
    def isBranch: Boolean = op match {
      case _:Switch[_] | _:SwitchCase[_] | _:IfThenElse[_] => true
      case _ => false
    }

    def isParallel: Boolean = op.isInstanceOf[ParallelPipe]

    def isUnitPipe: Boolean = op.isInstanceOf[UnitPipe]

    def isStreamLoad: Boolean = op match {
      case _:FringeDenseLoad[_,_] => true
      case _ => false
    }

    def isTileTransfer: Boolean = op match {
      case _:FringeDenseLoad[_,_]   => true
      case _:FringeDenseStore[_,_]  => true
      case _:FringeSparseLoad[_,_]  => true
      case _:FringeSparseStore[_,_] => true
      case _ => false
    }

    // TODO[3]: Should this just be any write?
    def isParEnq: Boolean = op match {
      case _:FIFOBankedEnq[_] => true
      case _:LIFOBankedPush[_] => true
      case _:SRAMBankedWrite[_,_] => true
      case _:FIFOEnq[_] => true
      case _:LIFOPush[_] => true
      case _:SRAMWrite[_,_] => true
      //case _:ParLineBufferEnq[_] => true
      case _ => false
    }

    def isStreamStageEnabler: Boolean = op match {
      case _:FIFODeq[_] => true
      case _:FIFOBankedDeq[_] => true
      case _:LIFOPop[_] => true
      case _:LIFOBankedPop[_] => true
      case _:StreamInRead[_] => true
      case _:StreamInBankedRead[_] => true
      case _ => false
    }

    def isStreamStageHolder: Boolean = op match {
      case _:FIFOEnq[_] => true
      case _:FIFOBankedEnq[_] => true
      case _:LIFOPush[_] => true
      case _:LIFOBankedPush[_] => true
      case _:StreamOutWrite[_] => true
      case _:StreamOutBankedWrite[_] => true
      case _ => false
    }
  }

  class HierarchyControlOps(s: Option[Sym[_]]) {
    private def op: Option[Op[_]] = s.flatMap{sym => sym.op : Option[Op[_]] }

    def isControl: Boolean = op.exists(_.isControl)
    def isPrimitive: Boolean = op.exists(_.isPrimitive)
    def isEphemeral: Boolean = op.exists(_.isEphemeral)

    def isAccel: Boolean = op.exists(_.isAccel)
    def isSwitch: Boolean = op.exists(_.isSwitch)
    def isBranch: Boolean = op.exists(_.isBranch)
    def isParallel: Boolean = op.exists(_.isParallel)
    def isUnitPipe: Boolean = op.exists(_.isUnitPipe)

    def isStreamLoad: Boolean = op.exists(_.isStreamLoad)
    def isTileTransfer: Boolean = op.exists(_.isTileTransfer)

    def isParEnq: Boolean = op.exists(_.isParEnq)

    def isStreamStageEnabler: Boolean = op.exists(_.isStreamStageEnabler)
    def isStreamStageHolder: Boolean = op.exists(_.isStreamStageHolder)

  }

  implicit class SymHierarchy(s: Sym[_]) extends HierarchyControlOps(Some(s))
  implicit class CtrlHierarchy(ctrl: Ctrl) extends HierarchyControlOps(ctrl.s)

}

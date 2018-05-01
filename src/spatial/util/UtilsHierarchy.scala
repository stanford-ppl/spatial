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
    def isLoop: Boolean = op.isInstanceOf[Loop[_]]

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

  implicit class SymHierarchy(s: Sym[_]) {
    def isControl: Boolean = s.op.exists(_.isControl)
    def isPrimitive: Boolean = s.op.exists(_.isPrimitive)
    def isEphemeral: Boolean = s.op.exists(_.isEphemeral)

    def isAccel: Boolean = s.op.exists(_.isAccel)
    def isLoop: Boolean = s.op.exists{ _.isLoop }
    def isSwitch: Boolean = s.op.exists(_.isSwitch)
    def isBranch: Boolean = s.op.exists(_.isBranch)
    def isParallel: Boolean = s.op.exists(_.isParallel)
    def isUnitPipe: Boolean = s.op.exists(_.isUnitPipe)

    def isStreamLoad: Boolean = s.op.exists(_.isStreamLoad)
    def isTileTransfer: Boolean = s.op.exists(_.isTileTransfer)

    def isParEnq: Boolean = s.op.exists(_.isParEnq)

    def isStreamStageEnabler: Boolean = s.op.exists(_.isStreamStageEnabler)
    def isStreamStageHolder: Boolean = s.op.exists(_.isStreamStageHolder)

  }

  implicit class CtrlHierarchy(ctrl: Ctrl) {
    def isAccel: Boolean = ctrl.s.exists(_.isAccel)
    def isLoop: Boolean = ctrl.s.exists(_.isLoop)
    def isSwitch: Boolean = ctrl.s.exists(_.isSwitch)
    def isBranch: Boolean = ctrl.s.exists(_.isBranch)
    def isParallel: Boolean = ctrl.s.exists(_.isParallel)
  }

}

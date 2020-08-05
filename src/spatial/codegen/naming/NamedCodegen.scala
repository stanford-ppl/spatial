package spatial.codegen.naming

import argon._
import argon.node._
import argon.codegen.Codegen
import spatial.util.spatialConfig

import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.memory._

trait NamedCodegen extends Codegen {

  // Quote for sym without looking up in scoped
  def local(s: Sym[_]): String = s"${s}"

  def memNameOr(s: Sym[_], default: String): String = {
    // Hacky
    s"${s}_${s.nameOr(default)}${s.explicitName.getOrElse("").replace("Const(","").replace("\"","").replace(")","")}"
  }

  override def named(s: Sym[_], id: Int): String = s.op match {
    case Some(rhs) => rhs match {
      case _: AccelScope       => if (s.isInnerControl) s"${s}_inr_RootController${s._name}" else s"${s}_outr_RootController${s._name}"
      case _: UnitPipe         => if (s.isInnerControl) s"${s}_inr_UnitPipe${s._name}" else s"${s}_outr_UnitPipe${s._name}"
      case _: UnrolledForeach  => if (s.isInnerControl) s"${s}_inr_Foreach${s._name}" else s"${s}_outr_Foreach${s._name}"
      case _: UnrolledReduce   => if (s.isInnerControl) s"${s}_inr_Reduce${s._name}" else s"${s}_outr_Reduce${s._name}"
      case _: Switch[_]        => if (s.isInnerControl) s"${s}_inr_Switch${s._name}" else s"${s}_outr_Switch${s._name}"
      case _: SwitchCase[_]    => if (s.isInnerControl) s"${s}_inr_SwitchCase${s._name}" else s"${s}_outr_SwitchCase${s._name}"
      case _: StateMachine[_]  => if (s.isInnerControl) s"${s}_inr_FSM${s._name}" else s"${s}_outr_FSM${s._name}"
      case _: CounterNew[_]    => s"${s}_ctr"
      case _: CounterChainNew  => s"${s}_ctrchain"

      case _: IfThenElse[_]    => s"${s}_IfThenElse"

      case DRAMHostNew(_,_) => s"${s}_${s.nameOr("dramhost")}"
      case DRAMAccelNew(_) => s"${s}_${s.nameOr("dramaccel")}"
      case ArgInNew(_)  => s"${s}_${s.nameOr("argIn")}"
      case ArgOutNew(_) => s"${s}_${s.nameOr("argOut")}"
      case RegNew(_)    => s"${memNameOr(s,"reg")}"
      case FIFORegNew(_)    => s"${memNameOr(s,"fiforeg")}"
      case RegFileNew(_,_) => s"${memNameOr(s,"regfile")}"
      case LineBufferNew(_,_,_) => s"${memNameOr(s,"linebuf")}"
      case FIFONew(_)   => s"${memNameOr(s,"fifo")}"
      case LIFONew(_)   => s"${memNameOr(s,"lifo")}"
      case SRAMNew(_)   => s"${memNameOr(s,"sram")}" + {if (spatialConfig.dualReadPort || s.isDualPortedRead) "_dualread" else ""}
      case LUTNew(_,_)  => s"${s}_${s.nameOr("lut")}"

      case SetReg(reg,_)      => s"${s}_${s.nameOr(src"set_${local(reg)}")}"
      case GetReg(reg)       => s"${s}_${s.nameOr(src"get_${local(reg)}")}"

      case RegRead(reg)      => s"${s}_${s.nameOr(src"rd_${local(reg)}")}"
      case RegWrite(reg,_,_) => s"${s}_${s.nameOr(src"wr_${local(reg)}")}"
      case FIFORegDeq(reg,_)      => s"${s}_${s.nameOr(src"deq_${local(reg)}")}"
      case FIFORegEnq(reg,_,_) => s"${s}_${s.nameOr(src"enq_${local(reg)}")}"

      case _:SRAMBankedRead[_,_]  => s"${s}_${s.nameOr("rd")}"
      case _:SRAMBankedWrite[_,_] => s"${s}_${s.nameOr("wr")}"

      case MergeBufferNew(_,_)   => s"${s}_${s.nameOr("mergeBuf")}"
      case MergeBufferBankedEnq(buf,_,_,_)   => s"${s}_${s.nameOr(src"enq_$buf")}"
      case MergeBufferBankedDeq(buf,_)   => s"${s}_${s.nameOr(src"deq_$buf")}"

      case FIFOBankedEnq(fifo,_,_)   => s"${s}_${s.nameOr(src"enq_${local(fifo)}")}"
      case FIFOBankedDeq(fifo,_)     => s"${s}_${s.nameOr(src"deq_${local(fifo)}")}"
      case FIFOIsEmpty(fifo,_)       => s"${s}_${s.nameOr(src"isEmpty_${local(fifo)}")}"
      case FIFOIsFull(fifo,_)        => s"${s}_${s.nameOr(src"isFull_${local(fifo)}")}"
      case FIFOIsAlmostEmpty(fifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_${local(fifo)}")}"
      case FIFOIsAlmostFull(fifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_${local(fifo)}")}"
      case FIFONumel(fifo,_)         => s"${s}_${s.nameOr(src"numel_${local(fifo)}")}"

      case LIFOBankedPush(lifo,_,_)  => s"${s}_${s.nameOr(src"push_${local(lifo)}")}"
      case LIFOBankedPop(lifo,_)     => s"${s}_${s.nameOr(src"pop_${local(lifo)}")}"
      case LIFOIsEmpty(lifo,_)       => s"${s}_${s.nameOr(src"isEmpty_${local(lifo)}")}"
      case LIFOIsFull(lifo,_)        => s"${s}_${s.nameOr(src"isFull_${local(lifo)}")}"
      case LIFOIsAlmostEmpty(lifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_${local(lifo)}")}"
      case LIFOIsAlmostFull(lifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_${local(lifo)}")}"
      case LIFONumel(lifo,_)         => s"${s}_${s.nameOr(src"numel_${local(lifo)}")}"

      case VecAlloc(_)           => s"${s}_vec"
      case VecApply(_,i)         => s"${s}_elem_$i"
      case VecSlice(_,start,end) => s"${s}_slice_${start}_to_$end"
      case _: SimpleStruct[_]    => s"${s}_tuple"
      case _: FieldApply[_,_]    => s"${s}_apply"

      case FixRandom(_) => s"${s}_fixrnd"
      case FixNeg(x)    => s"${s}_${s.nameOr(s"neg$x")}"
      case FixAdd(_,_)  => s"${s}_${s.nameOr("sum")}"
      case FixSub(_,_)  => s"${s}_${s.nameOr("sub")}"
      case FixDiv(_,_)  => s"${s}_${s.nameOr("div")}"
      case FixMul(_,_)  => s"${s}_${s.nameOr("mul")}"

      case _:SpatialCtrlBlackboxImpl[_,_]  => s"${s}_sctrlbox_${s.nameOr("impl")}"
      case SpatialCtrlBlackboxUse(_,box,_)  => s"${s}_${box}_${s.nameOr("use")}"
      case _:VerilogCtrlBlackbox[_,_]  => s"${s}_vctrlbox_${s.nameOr("use")}"
      case _:SpatialBlackboxImpl[_,_]  => s"${s}_sprimbox_${s.nameOr("impl")}"
      case SpatialBlackboxUse(box,_)  => s"${s}_${box}_${s.nameOr("use")}"
      case _:VerilogBlackbox[_,_]  => s"${s}_vprimbox_${s.nameOr("use")}"

      case DelayLine(size, data) if data.isConst => src"$data"
      // case DelayLine(size, data)                 => s"${s}_D$size"

      case _ => super.named(s,id)
    }
    case _ => super.named(s,id)
  }


}

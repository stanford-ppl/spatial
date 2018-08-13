package spatial.codegen.naming

import argon._
import argon.node._
import argon.codegen.Codegen

import spatial.lang._
import spatial.node._
import spatial.metadata.control._

trait NamedCodegen extends Codegen {

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

      case DRAMNew(_,_) => s"${s}_${s.nameOr("dram")}"
      case ArgInNew(_)  => s"${s}_${s.nameOr("argIn")}"
      case ArgOutNew(_) => s"${s}_${s.nameOr("argOut")}"
      case RegNew(_)    => s"${s}_${s.nameOr("reg")}"
      case RegFileNew(_,_) => s"${s}_${s.nameOr("regfile")}"
      case FIFONew(_)   => s"${s}_${s.nameOr("fifo")}"
      case LIFONew(_)   => s"${s}_${s.nameOr("lifo")}"
      case SRAMNew(_)   => s"${s}_${s.nameOr("sram")}"
      case LUTNew(_,_)  => s"${s}_${s.nameOr("lut")}"

      case SetReg(reg,_)      => s"${s}_${s.nameOr(src"set_$reg")}"
      case GetReg(reg)       => s"${s}_${s.nameOr(src"get_$reg")}"

      case RegRead(reg)      => s"${s}_${s.nameOr(src"rd_$reg")}"
      case RegWrite(reg,_,_) => s"${s}_${s.nameOr(src"wr_$reg")}"

      case _:SRAMBankedRead[_,_]  => s"${s}_${s.nameOr("rd")}"
      case _:SRAMBankedWrite[_,_] => s"${s}_${s.nameOr("wr")}"

      case FIFOBankedEnq(fifo,_,_)   => s"${s}_${s.nameOr(src"enq_$fifo")}"
      case FIFOBankedDeq(fifo,_)     => s"${s}_${s.nameOr(src"deq_$fifo")}"
      case FIFOIsEmpty(fifo,_)       => s"${s}_${s.nameOr(src"isEmpty_$fifo")}"
      case FIFOIsFull(fifo,_)        => s"${s}_${s.nameOr(src"isFull_$fifo")}"
      case FIFOIsAlmostEmpty(fifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_$fifo")}"
      case FIFOIsAlmostFull(fifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_$fifo")}"
      case FIFONumel(fifo,_)         => s"${s}_${s.nameOr(src"numel_$fifo")}"

      case LIFOBankedPush(lifo,_,_)  => s"${s}_${s.nameOr(src"push_$lifo")}"
      case LIFOBankedPop(lifo,_)     => s"${s}_${s.nameOr(src"pop_$lifo")}"
      case LIFOIsEmpty(lifo,_)       => s"${s}_${s.nameOr(src"isEmpty_$lifo")}"
      case LIFOIsFull(lifo,_)        => s"${s}_${s.nameOr(src"isFull_$lifo")}"
      case LIFOIsAlmostEmpty(lifo,_) => s"${s}_${s.nameOr(src"isAlmostEmpty_$lifo")}"
      case LIFOIsAlmostFull(lifo,_)  => s"${s}_${s.nameOr(src"isAlmostFull_$lifo")}"
      case LIFONumel(lifo,_)         => s"${s}_${s.nameOr(src"numel_$lifo")}"

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

      case DelayLine(size, data) if data.isConst => src"$data"
      case DelayLine(size, data)                 => s"${s}_D$size"

      case _ => super.named(s,id)
    }
    case _ => super.named(s,id)
  }


}

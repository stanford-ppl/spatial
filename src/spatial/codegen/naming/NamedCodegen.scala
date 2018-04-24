package spatial.codegen.naming

import argon._
import argon.codegen._

import spatial.lang._
import spatial.node._

import scala.collection.mutable

trait NamedCodegen extends Codegen {

  override def named(s: Sym[_], id: Int): String = s.op match {
    case Some(rhs) => rhs match {
      case _: AccelScope       => s"${s}_RootController${s._name}"
      case _: UnitPipe         => s"${s}_UnitPipe${s._name}"
      case _: UnrolledForeach  => s"${s}_Foreach${s._name}"
      case _: UnrolledReduce   => s"${s}_Reduce${s._name}"
      case _: Switch[_]        => s"${s}_Switch${s._name}"
      case _: SwitchCase[_]    => s"${s}_SwitchCase${s._name}"
      case _: StateMachine[_]  => s"${s}_FSM${s._name}"
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

      case _ => super.named(s,id)
    }
    case _ => super.named(s,id)
  }


}

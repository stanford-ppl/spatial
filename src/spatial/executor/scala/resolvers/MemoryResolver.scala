package spatial.executor.scala.resolvers

import argon._
import emul.FixedPoint
import spatial.executor.scala._
import spatial.executor.scala.memories.{ScalaReg, ScalaTensor}
import spatial.lang.Reg
import spatial.node._
import utils.Result.RunError

import scala.reflect.ClassTag

trait MemoryResolver extends OpResolverBase {

  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    sym match {
      case Op(ao:RegAlloc[_, Reg]) =>
        val tp: ExpType[_, _] = ao.A.tp
        type VT = tp.L

        val initVal = execState.getValue[VT](ao.init)
        implicit val ct: ClassTag[VT] = tp.tag.asInstanceOf[ClassTag[VT]]
        new ScalaReg[VT](initVal, initVal)

      case Op(RegWrite(mem, data, ens)) =>
        val tmp = execState(mem) match { case sr:ScalaReg[_] => sr }
        type ET = tmp.ET
        val sReg = tmp.asInstanceOf[ScalaReg[ET]]
        val eData = execState.getValue[ET](data)
        val enabled = ens.forall {
          bit => execState(bit) match {case en:EmulVal[Boolean] => en.value}
        }
        sReg.write(eData, enabled)
        EmulUnit(sym)

      case Op(RegRead(mem)) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]
        SimpleEmulVal(reg.curVal)

      case Op(GetReg(mem)) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]
        SimpleEmulVal(reg.curVal)

      case Op(SetReg(mem, data)) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]

        val realData = execState.getValue[ET](data)
        reg.write(realData, true)
        EmulUnit(sym)

      case Op(malloc: MemAlloc[_, _]) =>
        val elType: ExpType[_, _] = malloc.A.tp
        type ET = elType.L
        implicit val ct: ClassTag[ET] = elType.tag.asInstanceOf[ClassTag[ET]]
        new ScalaTensor[ET](malloc.dims.map(execState.getValue[FixedPoint](_)).map(_.toInt))

      case Op(sm@SetMem(dram, data)) =>
        val elType: ExpType[_, _] = sm.A.tp
        type ET = elType.L
        val target = execState.getTensor[ET](dram)
        val wrData = execState.getTensor[ET](data)
        if (wrData.shape != target.shape) {
          throw RunError(s"Mismatched SetMem shapes: ${target.shape} <- ${wrData.shape}")
        }
        wrData.values.copyToArray(target.values)
        EmulUnit(sym)

      case _ => super.run(sym, execState)
    }
  }
}

package spatial.executor.scala.resolvers

import argon._
import spatial.executor.scala._
import spatial.executor.scala.memories.ScalaReg
import spatial.lang.Reg
import spatial.node._

trait MemoryResolver extends OpResolverBase {

  override def run[U, V](sym: Exp[U, V], execState: ExecutionState): EmulResult = {
    sym match {
      case Op(ao:RegAlloc[_, Reg]) =>
        val tp = ao.A.tp
        type VT = tp.L

        val initVal = execState(ao.init).asInstanceOf[EmulVal[VT]]
        new ScalaReg(sym, initVal, initVal)

      case Op(RegWrite(mem, data, ens)) =>
        val tmp = execState(mem) match { case sr:ScalaReg[_] => sr }
        type ET = tmp.ET
        val sReg = tmp.asInstanceOf[ScalaReg[ET]]
        val eData: EmulVal[ET] = execState(data) match { case value: EmulVal[ET] => value }
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
        SimpleEmulVal(sym, reg.curVal)

      case Op(GetReg(mem)) =>
        val tmp = execState(mem) match {
          case sr: ScalaReg[_] => sr
        }
        type ET = tmp.ET
        val reg = tmp.asInstanceOf[ScalaReg[ET]]
        SimpleEmulVal(sym, reg.curVal)

      case _ => super.run(sym, execState)
    }
  }
}

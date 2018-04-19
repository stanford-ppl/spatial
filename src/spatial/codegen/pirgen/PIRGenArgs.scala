package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._
import spatial.data._

trait PIRGenArgs extends PIRGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: ArgIn[_]  => src"Array[${tp.A}]"
    case tp: ArgOut[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArgInNew(init) => 
      emitc(lhs, src"top.argFringe.argIn(init=${getConstant(init).get})", rhs)
      boundOf.get(lhs).foreach { bound =>
        emit(src"boundOf(${lhs}) = ${bound}")
      }
    case SetArgIn(reg, v)  => emitDummy(lhs, rhs)
    case ArgInRead(reg)    => emitDummy(lhs, rhs)

    case op@ArgOutNew(init)    => 
        emitc(lhs, src"top.argFringe.argOut(init=${getConstant(init).get})", rhs)
    case ArgOutWrite(reg,v,en) => emitDummy(lhs, rhs)
    case GetArgOut(reg)        => emitDummy(lhs, rhs)
    case _ => super.gen(lhs, rhs)
  }

}

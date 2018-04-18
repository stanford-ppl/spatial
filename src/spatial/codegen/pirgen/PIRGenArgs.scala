package spatial.codegen.pirgen

import argon._
import spatial.lang._
import spatial.node._

trait PIRGenArgs extends PIRGenMemories {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: ArgIn[_]  => src"Array[${tp.A}]"
    case tp: ArgOut[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArgInNew(init) => 
      //emit(quote(lhs, 0), s"top.argFringe.argIn(init=${getConstant(init).get})", rhs)
      //boundOf.get(lhs).foreach { bound =>
        //emit(s"boundOf(${quote(lhs, 0)}) = ${bound}")
      //}
    case SetArgIn(reg, v)  => emit(src"val $lhs = $reg.set($v)")
    case ArgInRead(reg)    => emit(src"val $lhs = $reg.value")

    case op@ArgOutNew(init)    => emitMemObject(lhs){ emit(src"object $lhs extends Ptr[${op.A}]($init)") }
    case ArgOutWrite(reg,v,en) => emit(src"val $lhs = if (${and(en)}) $reg.set($v)")
    case GetArgOut(reg)        => emit(src"val $lhs = $reg.value")
    case _ => super.gen(lhs, rhs)
  }

}

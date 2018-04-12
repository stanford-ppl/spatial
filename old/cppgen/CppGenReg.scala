package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._

import spatial.metadata._
import spatial.nodes._


trait CppGenReg extends CppGenSRAM {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(ArgInNew(_))  => s"${s}_argin"
    case Def(ArgOutNew(_)) => s"${s}_argout"
    case Def(RegNew(_))    => s"""${s}_${s.name.getOrElse("reg")}"""
    case Def(RegRead(reg:Sym[_]))      => s"${s}_readx${reg.id}"
    case Def(RegWrite(reg:Sym[_],_,_)) => s"${s}_writex${reg.id}"
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: RegType[_] => src"${tp.typeArguments.head}"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArgInNew(init)  => 
      argIns = argIns :+ lhs
      emit(src"${lhs.tp} $lhs = 0; // Initialize cpp argin ???")
    case ArgOutNew(init) => 
      argOuts = argOuts :+ lhs
      emit(src"//${lhs.tp}* $lhs = new int32_t {0}; // Initialize cpp argout ???")
    case HostIONew(init) => 
      argIOs = argIOs :+ lhs.asInstanceOf[Sym[Reg[_]]]
      emit(src"${lhs.tp} $lhs = 0; // Initialize cpp argout ???")
    case RegRead(reg)    => 
      emit(src"${lhs.tp} $lhs = $reg;")
    case RegWrite(reg,v,en) => 
      emit(src"// $lhs $reg $v $en reg write")
    case _ => super.gen(lhs, rhs)
  }

  override protected def emitFileFooter() {
    withStream(getStream("ArgAPI", "h")) {
      emit("\n// ArgIns")
      argIns.foreach{a => emit(src"#define ${argHandle(a)}_arg ${argMapping(a)._2}")}
      emit("\n// ArgOuts")
      argOuts.foreach{a => emit(src"#define ${argHandle(a)}_arg ${argMapping(a)._3}")}
      emit("\n// ArgIOs")
      argIOs.foreach{a => emit(src"#define ${argHandle(a)}_arg ${argMapping(a)._2}")}
    }
    super.emitFileFooter()
  }
}

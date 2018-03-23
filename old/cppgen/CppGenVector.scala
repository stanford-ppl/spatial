package spatial.codegen.cppgen

import argon.codegen.cppgen.CppCodegen
import argon.core._
import argon.nodes._
import spatial.aliases._
import spatial.nodes._


trait CppGenVector extends CppCodegen {

  override protected def name(s: Dyn[_]): String = s match {
    case Def(ListVector(_))            => s"${s}_vecified"
    case Def(VectorApply(_,i))         => s"${s}_elem$i"
    case Def(VectorSlice(_,start,end)) => s"${s}_slice${start}to$end"
    case _ => super.name(s)
  } 

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: VectorType[_] => src"vector<${tp.typeArguments.head}>"
    case _ => super.remap(tp)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ListVector(elems)      => emit(src"${lhs.tp} $lhs = {" + elems.map(quote).mkString(",") + "};")
    case VectorApply(vector, i) => emit(src"${lhs.tp} $lhs = $vector >> $i;")
    case VectorSlice(vector, start, end) => emit(src"${lhs.tp} $lhs;")
                open(src"""for (int ${lhs}_i = 0; ${lhs}_i < ${start} - ${end} + 1; ${lhs}_i++){""") 
                  emit(src"""  bool ${lhs}_temp = (${vector} >> ${lhs}_i) & 1; """)
                  emit(src"""  ${lhs}.push_back(${lhs}_temp); """)
                close("}")
    case e@DataAsBits(a) => e.mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,d,f) => emit(src"${e.mT} $lhs = (${e.mT}) ${a};")
      case BooleanType()    => emit(src"${e.mT} $lhs = (${e.mT}) ${a};")
    }

    case BitsAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) => 
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
      case BooleanType() =>
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < 1; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
    }

    case _ => super.emitNode(lhs, rhs)
  }
}
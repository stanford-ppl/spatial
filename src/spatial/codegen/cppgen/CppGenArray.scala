package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._


trait CppGenArray extends CppGenCommon {

  protected def getSize(array: Sym[_], extractor:String = ""): String = {
    src"(*${array})${extractor}.size()"
  }

  protected def emitNewArray(lhs: Sym[_], tp: Type[_], size: String): Unit = {
    emit(src"${tp}* $lhs = new ${tp}($size);")
  }

  protected def emitApply(dst: Sym[_], array: Sym[_], i: String, isDef: Boolean = true): Unit = {
    // if (isArrayType(dst.tp)) {
    //   val iterator = if ("^[0-9].*".r.findFirstIn(src"$i").isDefined) {src"${array}_applier"} else {src"$i"}
    //   if (isDef) {
    //     emit(src"""${dst.tp}* $dst = new ${dst.tp}(${getSize(array, src"[$i]")}); //cannot apply a vector from 2D vector, so make new vec and fill it, eventually copy the vector in the constructor here""")
    //     emit(src"for (int ${iterator}_sub = 0; ${iterator}_sub < (*${array})[${i}].size(); ${iterator}_sub++) { (*$dst)[${iterator}_sub] = (*${array})[$i][${iterator}_sub]; }")          
    //   } else {
    //     emit(src"for (int ${iterator}_sub = 0; ${iterator}_sub < (*${array})[${i}].size(); ${iterator}_sub++) { (*$dst)[${iterator}_sub] = (*${array})[$i][${iterator}_sub]; }")          
    //   }
    // } else {
    //   if (isDef) {
    //     emit(src"${dst.tp} $dst = (*${array})[$i];")  
    //   } else {
    //     emit(src"$dst = (*${array})[$i];")
    //   }
    // }
  }


  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case InputArguments()       => emit(src"${lhs.tp}* $lhs = args;")
    case ArrayApply(array, i)   => emit(src"${lhs.tp} $lhs = (*${array})[$i];")  
    case op@ArrayNew(size)      => emitNewArray(lhs, lhs.tp, src"$size")
    case ArrayLength(array)     => emit(src"${lhs.tp} $lhs = ${getSize(array)};")
    case DataAsBits(bits)       => emit(src"${lhs.tp} $lhs = (${lhs.tp}) $bits;")
    case BitsAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) => 
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
      // case BooleanType() =>
      //   emit(src"${lhs.tp} $lhs=0;")
      //   emit(src"for (int ${lhs}_i = 0; ${lhs}_i < 1; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
    }
    case VecApply(vector, i) => emit(src"${lhs.tp} $lhs = $vector >> $i;")
    case VecSlice(vector, start, end) => emit(src"${lhs.tp} $lhs;")
                open(src"""for (int ${lhs}_i = 0; ${lhs}_i < ${start} - ${end} + 1; ${lhs}_i++){""") 
                  emit(src"""  bool ${lhs}_temp = (${vector} >> ${lhs}_i) & 1; """)
                  emit(src"""  ${lhs}.push_back(${lhs}_temp); """)
                close("}")
    case VecConcat(elems) => 
      emit(src"${lhs.tp} $lhs = vector<${lhs.tp}>(${elems.length});")
      elems.foreach{e => emit(src"${lhs}.push_back($e);")}



    case _ => super.gen(lhs, rhs)
  }


}

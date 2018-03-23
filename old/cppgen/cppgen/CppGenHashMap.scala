package argon.codegen.cppgen

import argon.core._
import argon.nodes._

trait CppGenHashMap extends CppGenArrayExt {

  // override protected def remap(tp: Type[_]): String = tp match {
  //   case HashIndexType(mK) => src"scala.collection.mutable.HashMap[$mK,${typ[Index]}]"
  //   case _ => super.remap(tp)
  // }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]) = rhs match {
    case HashIndexApply(index, key) => 
      emit(src"${lhs.tp} $lhs = -1;")
      open(src"for (int ${lhs}_i = 0; ${lhs}_i < ${index}_size; ${lhs}_i++) {")
        emit(src"if ((*${index})[${lhs}_i] == ${key}) { $lhs = ${lhs}_i; }")
      close("}")
      emit(src"// assert(${lhs} > -1); // Allow index lookup errors")
    case _ => super.emitNode(lhs, rhs)
  }

  override protected def emitFat(lhs: Seq[Sym[_]], rhs: Def) = rhs match {
    case e @ BuildHashMap(in, apply, keyFunc, valFunc, reduce, rV, i) =>
      emit(src"vector<${e.mK}>* ${lhs(0)} = new vector<${e.mK}>((*${in}).size()); // Keys")
      emit(src"vector<${e.mV}>* ${lhs(1)} = new vector<${e.mV}>((*${in}).size()); // Values")
      emit(src"vector<${e.mK}>* ${lhs(2)} = ${lhs(0)}; // TODO: Probably totally wrong.  lhs2 appears to be the scala hashmap, but it seems like we only use it to lookup index of a key")
      emit(src"long ${lhs(2)}_size = 0;")
      //emit(src"//val ${lhs(2)}  = new ${HashIndexType(e.mK)}()")
      open(src"for (int $i = 0; $i < (*${in}).size(); $i++) { ")
        emit(s"// Apply block")
        visitBlock(apply)
        emit(s"// Key block")
        visitBlock(keyFunc)
        emit(s"// Val block")
        visitBlock(valFunc)
        emit(s"bool contained = false;")
        emit(s"long idx = -1;")
        emit(src"for (int i = 0; i < ${lhs(2)}_size; i++) {if ((*${lhs(0)})[i] == ${keyFunc.result}) {contained = true; idx = i;}}")
        open(src"if (contained) {")
          emit(src"${rV._1.tp}* ${rV._1} = ${valFunc.result};")

          emit(src"${rV._2.tp}* ${rV._2} = new ${rV._2.tp}((*${lhs(1)})[idx].size());")
          open(src"for (int ${rV._2}_copier = 0; ${rV._2}_copier < ${getSize(rV._2)}; ${rV._2}_copier++) {")
            emit(src"(*${rV._2})[${rV._2}_copier] = (*${lhs(1)})[idx][${rV._2}_copier];")
          close("}")

          visitBlock(reduce)
          emitUpdate(lhs(1), reduce.result, "idx", reduce.result.tp)
        closeopen("} else {")
          emit(src"//index += ${keyFunc.result} -> ${lhs(1)}.${lhs(2)}_size")
          emitUpdate(lhs(0), keyFunc.result, src"${lhs(2)}_size", keyFunc.result.tp)
          emitUpdate(lhs(1), valFunc.result, src"${lhs(2)}_size", valFunc.result.tp)
          emit(src"${lhs(2)}_size += 1;")
        close("}")
      close("}")

    case _ => super.emitFat(lhs, rhs)
  }

}

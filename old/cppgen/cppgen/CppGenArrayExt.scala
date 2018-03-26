package argon.codegen.cppgen

import argon.core._
import argon.NDArrayException
import argon.nodes._

trait CppGenArrayExt extends CppGenArray {

  private def getNestingLevel(tp: Type[_]): Int = tp match {
    case tp: ArrayType[_] => 1 + getNestingLevel(tp.typeArguments.head) 
    case _ => 0
  }

  private def zeroElement(tp: Type[_]): String = tp match {
    case tp: Tuple2Type[_,_] => src"*(new ${tp}(0,0));"
    case _ => "0"
  }
 
  private def getPrimitiveType(tp: Type[_]): String = tp match {
    case tp: ArrayType[_] => getPrimitiveType(tp.typeArguments.head) 
    case _ => remap(tp)
  }

  protected def emitUpdate(lhs: Exp[_], value: Exp[_], i: String, tp: Type[_]): Unit = {
    if (isArrayType(tp)) {
      if (getNestingLevel(tp) > 1) throw new NDArrayException(lhs, src"$tp")

      emit(src"(*$lhs)[$i].resize(${getSize(value)});")
      open(src"for (int ${value}_copier = 0; ${value}_copier < ${getSize(value)}; ${value}_copier++) {")
        emit(src"(*$lhs)[$i][${value}_copier] = (*${value})[${value}_copier];")
      close("}")
    }
    else {
      tp match {
        case FixPtType(s,d,f) if (f == 0) => 
          val intermediate_tp = if (d+f > 64) s"int128_t"
                   else if (d+f > 32) s"int64_t"
                   else if (d+f > 16) s"int32_t"
                   else if (d+f > 8) s"int16_t"
                   else if (d+f > 4) s"int8_t"
                   else if (d+f > 2) s"int8_t"
                   else if (d+f == 2) s"int8_t"
                   else "bool"

          emit(src"(*$lhs)[$i] = (${intermediate_tp}) ${value};") // Always convert to signed, then to unsigned if on the boards
        case _ => emit(src"(*$lhs)[$i] = ${value};")
      }
    }
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case ArrayUpdate(array, i, data) => emitUpdate(array, data, src"$i", data.tp)
    case MapIndices(size, func, i)   =>
      emitNewArray(lhs, lhs.tp, src"$size")
      open(src"for (int $i = 0; $i < $size; ${i}++) {")
      emitBlock(func)
      if (isArrayType(func.result.tp)) {
        if (getNestingLevel(func.result.tp) > 1) {Console.println(s"ERROR: Need to fix more than 2D arrays")}
        emit(src"(*$lhs)[$i].resize(${getSize(func.result)});")
        open(src"for (int ${func.result}_copier = 0; ${func.result}_copier < ${getSize(func.result)}; ${func.result}_copier++) {")
          emit(src"(*$lhs)[$i][${func.result}_copier] = (*${func.result})[${func.result}_copier];")
        close("}")
      } else {
        emit(src"(*$lhs)[$i] = ${func.result};")  
      }
      close("}")

    case ArrayForeach(array,apply,func,i) =>
      open(src"for (int ${i} = 0; ${i} < ${getSize(array)}; ${i} = ${i} + 1) {")
      visitBlock(apply)
      emitBlock(func)
      close("}")

    case ArrayMap(array,apply,func,i) =>
      emitNewArray(lhs, lhs.tp, getSize(array))
      open(src"for (int $i = 0; $i < ${getSize(array)}; $i++) { ")
      visitBlock(apply)
      emitBlock(func)
      emitUpdate(lhs, func.result, src"$i", func.result.tp)
      close("}")
      

    case ArrayZip(a, b, applyA, applyB, func, i) =>
      emitNewArray(lhs, lhs.tp, getSize(a))
      open(src"for (int $i = 0; $i < ${getSize(a)}; ${i}++) { ")
      visitBlock(applyA)
      visitBlock(applyB)
      emitBlock(func)
      emitUpdate(lhs, func.result, src"$i", func.result.tp)
      close("}")

      // 
    case ArrayReduce(array, apply, reduce, i, rV) =>

      if (isArrayType(lhs.tp)) {
        emit(src"""${lhs.tp}* $lhs = new ${lhs.tp}(${getSize(array, "[0]")});""") 
      } else {
        emit(src"${lhs.tp} $lhs;") 
      }
      open(src"if (${getSize(array)} > 0) { // Hack to handle reductions on things of length 0")
        emitApply(lhs, array, "0", false)
      closeopen("} else {")
        emit(src"$lhs = ${zeroElement(lhs.tp)};")
      close("}")
      open(src"for (int $i = 1; $i < ${getSize(array)}; ${i}++) {")
        emitApply(rV._1, array, src"$i")
        // emit(src"""${rV._1.tp}${if (isArrayType(rV._1.tp)) "*" else ""} ${rV._1} = (*${array})[$i];""")
        emit(src"""${rV._2.tp}${if (isArrayType(rV._2.tp)) "*" else ""} ${rV._2} = $lhs;""")
        emitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("}")

    case ArrayFilter(array, apply, cond, i) =>
      open(src"val $lhs = $array.filter{${apply.result} => ")
      emitBlock(cond)
      close("}")

    case ArrayFlatMap(array, apply, func, i) =>
      val nesting = getNestingLevel(array.tp)
      emit("// TODO: flatMap node assumes the func block contains only applies (.flatten)")

      // Initialize lhs array
      (0 until nesting).foreach{ level =>
        val grabbers = (0 until level).map{ m => "[0]" }.mkString("")
        emit(src"int size_${lhs}_$level = (*${array})${grabbers}.size();")
      }
      emitNewArray(lhs, lhs.tp, src"""${(0 until nesting).map{ m => src"size_${lhs}_$m" }.mkString("*")}""")

      // Open all levels of loop
      (0 until nesting).foreach { level => 
        val grabbers = (0 until level).map{ "[0]" }.mkString("") 
        open(src"for (int ${i}_$level = 0; ${i}_${level} < size_${lhs}_$level; ${i}_${level}++) { ")
      }

      // Pluck off elements of the $array
      val applyString = (0 until nesting).map{ level => src"""[${i}_${level}]""" }.mkString("")
      emit(src"${getPrimitiveType(lhs.tp)} ${func.result} = (*${array})${applyString};")

      // Update the lhs
      val flatIndex = (0 until nesting).map{ level => 
        src"""${ (level+1 until nesting).map{ k => src"size_${lhs}_$k" }.mkString("*") } ${ if (level+1 < nesting) "*" else "" }${i}_${level}"""
      }.mkString(" + ")
      emit(src"(*$lhs)[$flatIndex] = ${func.result};")

      // Close all levels of loop
      (0 until nesting).foreach { level => 
        val grabbers = (0 until level).map{ "[0]" }.mkString("") 
        close("}")
      }


    case _ => super.gen(lhs, rhs)
  }
}

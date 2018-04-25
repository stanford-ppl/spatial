package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import spatial.data._
import spatial.util._


trait CppGenArray extends CppGenCommon {

  var struct_list: List[String] = List()

  protected def getSize(array: Sym[_], extractor:String = ""): String = {
    src"(*${array})${extractor}.size()"
  }

  protected def emitNewArray(lhs: Sym[_], tp: Type[_], size: String): Unit = {
    emit(src"${tp}* $lhs = new ${tp}($size);")
  }

  private def getNestingLevel(tp: Type[_]): Int = tp match {
    case tp: Vec[_] => 1 + getNestingLevel(tp.typeArgs.head) 
    case _ => 0
  }

  protected def isArrayType(tp: Type[_]): Boolean = tp match {
    case tp: Vec[_] => tp.typeArgs.head match {
      case tp: Vec[_] => println("EXCEPTION: Probably can't handle nested array types in ifthenelse"); true
      case _ => true
    }
    case _ => tp.typePrefix == "Array"
  }

  protected def emitUpdate(lhs: Sym[_], value: Sym[_], i: String, tp: Type[_]): Unit = {
    if (isArrayType(tp)) {
      if (getNestingLevel(tp) > 1) throw new Exception(s"ND Array exception on $lhs, $tp")

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

  protected def emitApply(dst: Sym[_], array: Sym[_], i: String, isDef: Boolean = true): Unit = {
    if (isArrayType(dst.tp)) {
      val iterator = if ("^[0-9].*".r.findFirstIn(src"$i").isDefined) {src"${array}_applier"} else {src"$i"}
      if (isDef) {
        emit(src"""${dst.tp}* $dst = new ${dst.tp}(${getSize(array, src"[$i]")}); //cannot apply a vector from 2D vector, so make new vec and fill it, eventually copy the vector in the constructor here""")
        emit(src"for (int ${iterator}_sub = 0; ${iterator}_sub < (*${array})[${i}].size(); ${iterator}_sub++) { (*$dst)[${iterator}_sub] = (*${array})[$i][${iterator}_sub]; }")          
      } else {
        emit(src"for (int ${iterator}_sub = 0; ${iterator}_sub < (*${array})[${i}].size(); ${iterator}_sub++) { (*$dst)[${iterator}_sub] = (*${array})[$i][${iterator}_sub]; }")          
      }
    } else {
      if (isDef) {
        emit(src"${dst.tp} $dst = (*${array})[$i];")  
      } else {
        emit(src"$dst = (*${array})[$i];")
      }
    }
  }

  private def getPrimitiveType(tp: Type[_]): String = tp match {
    case tp: Vec[_] => getPrimitiveType(tp.typeArgs.head) 
    case _ => remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case InputArguments()       => emit(src"${lhs.tp}* $lhs = args;")
    case ArrayApply(array, i)   => emit(src"${lhs.tp} $lhs = (*${array})[$i];")  
    case op@ArrayNew(size)      => emitNewArray(lhs, lhs.tp, src"$size")
    case ArrayLength(array)     => emit(src"${lhs.tp} $lhs = ${getSize(array)};")
    case DataAsBits(bits)       => 
      emit(src"${lhs.tp} $lhs;")
      emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${bitWidth(lhs.tp)}; ${lhs}_i++) { ${lhs}.push_back($bits >> ${lhs}_i & 1); }")
    case BitsAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) => 
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
      // case BooleanType() =>
      //   emit(src"${lhs.tp} $lhs=0;")
      //   emit(src"for (int ${lhs}_i = 0; ${lhs}_i < 1; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
    }
    case SimpleStruct(st) => 
      val struct = st.map{case (name, data) => src"${name}${data.tp}".replaceAll("[<|>]","")}.mkString("")
      // Add to struct header if not there already
      if (!struct_list.contains(struct)) {
        struct_list = struct_list :+ struct
        inGen(out, "structs.hpp") {
          open(src"struct ${struct} {")
          st.foreach{f => emit(src"${f._2.tp}* ${f._1};")}
          open(src"${struct}(${st.map{f => src"${f._2.tp}* ${f._1}_in"}.mkString(",")}){")
            st.foreach{f => emit(src"${f._1} = ${f._1}_in;")}
          close("}")
          close("};")
        }
      }
      val fields = st.zipWithIndex.map{case (f,i) => 
        if (f._2.isConst) {
          emit(src"${f._2.tp} ${lhs}_$i = ${f._2};")
          src"&${lhs}_$i"
        } else if (isArrayType(f._2.tp) | f._2.tp.typePrefix == "Array") src"${f._2}"
        else src"&${f._2}"
      }
      emit(src"${struct} $lhs = ${struct}(${fields.mkString(",")});")

    case FieldApply(struct, field) => 
      if (isArrayType(lhs.tp)) emit(src"""${lhs.tp}* $lhs = ${struct}.$field;""")
      else emit(src"""${lhs.tp} $lhs = *${struct}.$field;""")

    case SetMem(dram, data) => 
      val f = fracBits(dram.tp.typeArgs.head)
      if (f > 0) {
        emit(src"vector<${dram.tp.typeArgs.head}>* ${dram}_rawified = new vector<${dram.tp.typeArgs.head}>((*${data}).size());")
        open(src"for (int ${dram}_rawified_i = 0; ${dram}_rawified_i < (*${data}).size(); ${dram}_rawified_i++) {")
          emit(src"(*${dram}_rawified)[${dram}_rawified_i] = (${dram.tp.typeArgs.head}) ((*${data})[${dram}_rawified_i] * ((${dram.tp.typeArgs.head})1 << $f));")
        close("}")
        emit(src"c1->memcpy($dram, &(*${dram}_rawified)[0], (*${dram}_rawified).size() * sizeof(${dram.tp.typeArgs.head}));")
      } else {
        emit(src"c1->memcpy($dram, &(*${data})[0], (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")
      }
    case GetMem(dram, data) => 
      val f = fracBits(dram.tp.typeArgs.head)
      if (f > 0) {
        emit(src"vector<${dram.tp.typeArgs.head}>* ${data}_rawified = new vector<${dram.tp.typeArgs.head}>((*${data}).size());")
        emit(src"c1->memcpy(&(*${data}_rawified)[0], $dram, (*${data}_rawified).size() * sizeof(${dram.tp.typeArgs.head}));")
        open(src"for (int ${data}_i = 0; ${data}_i < (*${data}).size(); ${data}_i++) {")
          emit(src"${dram.tp.typeArgs.head} ${data}_tmp = (*${data}_rawified)[${data}_i];")
          emit(src"(*${data})[${data}_i] = (double) ${data}_tmp / ((${dram.tp.typeArgs.head})1 << $f);")
        close("}")
      } else {
        emit(src"c1->memcpy(&(*$data)[0], $dram, (*${data}).size() * sizeof(${dram.tp.typeArgs.head}));")
      }

    case VecApply(vector, i) => emit(src"${lhs.tp} $lhs = $vector[$i];")
    case VecSlice(vector, start, end) => emit(src"${lhs.tp} $lhs;")
                open(src"""for (int ${lhs}_i = 0; ${lhs}_i < ${start} - ${end} + 1; ${lhs}_i++){""") 
                  emit(src"""bool ${lhs}_temp = (${vector}[${lhs}_i]); """)
                  emit(src"""${lhs}.push_back(${lhs}_temp); """)
                close("}")
    case VecConcat(elems) => 
      emit(src"${lhs.tp} $lhs;")
      elems.foreach{e => e.tp match {
        case _: Vec[_] => emit(src"${lhs}.push_back($e[0]);")
        case _ => emit(src"${lhs}.push_back($e);")
      }}

    case MapIndices(size, func)   =>
      emitNewArray(lhs, lhs.tp, src"$size")
      open(src"for (int ${func.input} = 0; ${func.input} < $size; ${func.input}++) {")
      visitBlock(func)
      if (isArrayType(func.result.tp)) {
        if (getNestingLevel(func.result.tp) > 1) {Console.println(s"ERROR: Need to fix more than 2D arrays")}
        emit(src"(*$lhs)[${func.input}].resize(${getSize(func.result)});")
        open(src"for (int ${func.result}_copier = 0; ${func.result}_copier < ${getSize(func.result)}; ${func.result}_copier++) {")
          emit(src"(*$lhs)[${func.input}][${func.result}_copier] = (*${func.result})[${func.result}_copier];")
        close("}")
      } else {
        emit(src"(*$lhs)[${func.input}] = ${func.result};")  
      }
      close("}")

    case SeriesForeach(start,end,step,func) =>
      open(src"for (int ${func.input} = $start; ${func.input} < ${end}; ${func.input} = ${func.input} + $step) {")
        visitBlock(func)
      close("}")

    case ArrayForeach(array,apply,func) =>
      open(src"for (int ${func.input} = 0; ${func.input} < ${getSize(array)}; ${func.input}++) {")
      visitBlock(apply)
      visitBlock(func)
      close("}")

    case ArrayMap(array,apply,func) =>
      emitNewArray(lhs, lhs.tp, getSize(array))
      open(src"for (int ${func.input} = 0; ${func.input} < ${getSize(array)}; ${func.input}++) { ")
      emit(src"int ${apply.inputB} = ${func.input};")
      visitBlock(apply)
      visitBlock(func)
      emitUpdate(lhs, func.result, src"${func.input}", func.result.tp)
      close("}")
      

    case ArrayZip(a, b, applyA, applyB, func) =>
      emitNewArray(lhs, lhs.tp, getSize(a))
      open(src"for (int ${applyA.inputB} = 0; ${applyA.inputB} < ${getSize(a)}; ${applyA.inputB}++) { ")
      visitBlock(applyA)
      visitBlock(applyB)
      visitBlock(func)
      emitUpdate(lhs, func.result, src"${applyA.inputB}", func.result.tp)
      close("}")

    case UnrolledForeach(ens,cchain,func,iters,valids) => 
      val starts = cchain.ctrs.map(_.start)
      val ends = cchain.ctrs.map(_.end)
      val steps = cchain.ctrs.map(_.step)
      iters.zipWithIndex.foreach{case (i,idx) => 
        open(src"for (int $i = ${starts(idx)}; $i < ${ends(idx)}; $i = $i + ${steps(idx)}) {")
        valids(idx).foreach{v => emit(src"${v.tp} ${v} = true; // TODO: Safe to assume this in cppgen?")}
      }
      visitBlock(func)
      iters.foreach{_ => close("}")}

      // 
    case ArrayReduce(array, apply, reduce) =>
      if (isArrayType(lhs.tp)) {
        emit(src"""${lhs.tp}* $lhs = new ${lhs.tp}(${getSize(array, "[0]")});""") 
      } else {
        emit(src"${lhs.tp} $lhs;") 
      }
      open(src"if (${getSize(array)} > 0) { // Hack to handle reductions on things of length 0")
        emitApply(lhs, array, "0", false)
      close("}")
      open("else {")
        emit(src"$lhs = 0;")
      close("}")

      open(src"for (int ${apply.inputB} = 1; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) {")
        emitApply(reduce.inputA, array, src"${apply.inputB}")
        emit(src"""${reduce.inputB.tp}${if (isArrayType(reduce.inputB.tp)) "*" else ""} ${reduce.inputB} = $lhs;""")
        visitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("}")

    case op@Switch(selects,block) =>

      emit(src"/** BEGIN SWITCH $lhs **/")
      emit(src"${lhs.tp} $lhs;")
      selects.indices.foreach { i =>
        open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) {""")
          visitBlock(op.cases(i).body)
          emit(src"${lhs} = ${op.cases(i).body.result};")
        close("}")
      }
      emit(src"/** END SWITCH $lhs **/")

    case SwitchCase(body) => // Controlled by Switch

    // case ArrayFilter(array, apply, cond) =>
    //   open(src"val $lhs = $array.filter{${apply.result} => ")
    //   visitBlock(cond)
    //   close("}")

    // case ArrayFlatMap(array, apply, func) =>
    //   val nesting = getNestingLevel(array.tp)
    //   emit("// TODO: flatMap node assumes the func block contains only applies (.flatten)")

    //   // Initialize lhs array
    //   List.tabulate(nesting){ level =>
    //     val grabbers = List.tabulate(level){ m => "[0]" }.mkString("")
    //     emit(src"int size_${lhs}_$level = (*${array})${grabbers}.size();")
    //     ()
    //   }
    //   emitNewArray(lhs, lhs.tp, src"""${List.tabulate(nesting){ m => src"size_${lhs}_$m" }.mkString("*")}""")

    //   // Open all levels of loop
    //   List.tabulate(nesting) { level => 
    //     val grabbers = List.tabulate(level){_ => "[0]" }.mkString("") 
    //     open(src"for (int i_$level = 0; i_${level} < size_${lhs}_$level; i_${level}++) { ")
    //     ()
    //   }

    //   // Pluck off elements of the $array
    //   val applyString = List.tabulate(nesting){ level => src"""[i_${level}]""" }.mkString("")
    //   emit(src"${getPrimitiveType(lhs.tp)} ${func.result} = (*${array})${applyString};")

    //   // Update the lhs
    //   val flatIndex = List.tabulate(nesting){ level => 
    //     src"""${ List.tabulate(nesting){ k => src"size_${lhs}_${level+1+k}" }.mkString("*") } ${ if (level+1 < nesting) "*" else "" }i_${level}"""
    //   }.mkString(" + ")
    //   emit(src"(*$lhs)[$flatIndex] = ${func.result};")

    //   // Close all levels of loop
    //   List.tabulate(nesting) { level => 
    //     val grabbers = List.tabulate(level){_ => "[0]" }.mkString("") 
    //     close("}")
    //     ()
    //   }
    case _ => super.gen(lhs, rhs)
  }


}

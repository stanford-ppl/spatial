package spatial.codegen.cppgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._

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

  private def zeroElement(tp: Type[_]): String = tp match {
    case tp: Tup2[_,_] => src"*(new $tp(${zeroElement(tp.A)},${zeroElement(tp.B)}));"
    case tp: Struct[_] => src"*(new $tp(${tp.fields.map{field => zeroElement(field._2)}.mkString(",")}));"
    case _ => "0"
  }

  protected def ptr(tp: Type[_]): String = if (isArrayType(tp)) "*" else ""
  protected def amp(tp: Type[_]): String = if (isArrayType(tp)) "&" else ""
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

    case ArrayApply(array @ Def(InputArguments()), i) =>
      // Note this doesn't match if we map over the input arguments and then apply, for example.
      emit(src"${lhs.tp} $lhs;")
      open(src"try {")
        emit(src"$lhs = (*$array).at($i);")
      close("}")
      open(src"catch (std::out_of_range& e) {")
        emit(src"printHelp();")
      close("}")

    case ArrayApply(array, i)   => 
      val (ast,amp) = if (lhs.tp match {case _:Vec[_] => true; case _:host.Array[_] => true; case _ => false}) ("*","&") else ("","")
      emit(src"${lhs.tp}${ast} $lhs = ${amp}(*${array})[$i];")  
    case op@ArrayNew(size)      => emitNewArray(lhs, lhs.tp, src"$size")
    case ArrayLength(array)     => emit(src"${lhs.tp} $lhs = ${getSize(array)};")
    case DataAsBits(bits)       => 
      bits.tp match {
        case FltPtType(_,_) =>
          emit(src"${lhs.tp} $lhs;")
          emit(src"float* ${lhs}_ptr = &${toTrueFix(quote(bits), bits.tp)};")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${bitWidth(lhs.tp)}; ${lhs}_i++) { ${lhs}.push_back(*reinterpret_cast<int32_t*>(${lhs}_ptr) >> ${lhs}_i & 1); }")
        case FixPtType(s,i,f) => 
          emit(src"${lhs.tp} $lhs;")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${bitWidth(lhs.tp)}; ${lhs}_i++) { ${lhs}.push_back(${toTrueFix(quote(bits), bits.tp)} >> ${lhs}_i & 1); }")
        case BitType() => 
          emit(src"${lhs.tp} $lhs;")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${bitWidth(lhs.tp)}; ${lhs}_i++) { ${lhs}.push_back(${toTrueFix(quote(bits), bits.tp)} >> ${lhs}_i & 1); }")
      }
    case BitsAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) => 
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i++) { if(${lhs}_i < ${v}.size()) {${lhs} += ${v}[${lhs}_i] << ${lhs}_i;} }")
    }
    case DataAsVec(bits)       =>
      val elwidth = bitWidth(lhs.tp.typeArgs.head)
      bits.tp match {
        case FltPtType(m,e) =>
          emit(src"${lhs.tp} $lhs;")
          emit(src"float* ${lhs}_ptr = &${toTrueFix(quote(bits), bits.tp)};")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${m+e}; ${lhs}_i = ${lhs}_i + $elwidth) { ${lhs}.push_back(*reinterpret_cast<int32_t*>(${lhs}_ptr) >> ${lhs}_i & ${(scala.math.pow(2,elwidth)-1).toInt}); }")
        case FixPtType(s,i,f) =>
          emit(src"${lhs.tp} $lhs;")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i = ${lhs}_i + $elwidth) { ${lhs}.push_back(${toTrueFix(quote(bits), bits.tp)} >> ${lhs}_i & ${(scala.math.pow(2,elwidth)-1).toInt}); }")
        case BitType() =>
          emit(src"${lhs.tp} $lhs;")
          emit(src"for (int ${lhs}_i = 0; ${lhs}_i < 1; ${lhs}_i = ${lhs}_i + $elwidth) { ${lhs}.push_back(${toTrueFix(quote(bits), bits.tp)} >> ${lhs}_i & ${(scala.math.pow(2,elwidth)-1).toInt}); }")
      }
    case VecAsData(v,mT) => mT match {
      case FltPtType(_,_)   => throw new Exception("Bit-wise operations not supported on floating point values yet")
      case FixPtType(s,i,f) =>
        val elwidth = bitWidth(v.tp.typeArgs.head)
        emit(src"${lhs.tp} $lhs=0;")
        emit(src"for (int ${lhs}_i = 0; ${lhs}_i < ${i+f}; ${lhs}_i = ${lhs}_i + $elwidth) { if(${lhs}_i < ${v}.size() * $elwidth) {${lhs} += (${lhs.tp}) ${v}[${lhs}_i/$elwidth] << ${lhs}_i;} }")
    }
    case SimpleStruct(st) => 
      // val struct = st.map{case (name, data) => src"${name}${data.tp}".replaceAll("[<|>]","")}.mkString("")
      val struct = src"${lhs.tp}".replaceAll("[<|>]","").replaceAll("[\\[\\],]", "_")
      // Add to struct header if not there already
      if (!struct_list.contains(struct)) {
        struct_list = struct_list :+ struct
        inGen(out, "structs.hpp") {
          open(src"struct ${struct} {")
            st.foreach{f => emit(src"${asIntType(f._2.tp)}${ptr(f._2.tp)} ${f._1}; // ${f._2.tp}")}
            open(src"${struct}(${st.map{f => src"${f._2.tp}${ptr(f._2.tp)} ${f._1}_in"}.mkString(",")}){ /* Normal Constructor */")
              st.foreach{f => emit(src"set${f._1}(${ptr(f._2.tp)}${f._1}_in);")}
            close("}")
            emit(src"${struct}(){} /* For creating empty array */")
            open(src"std::string toString(){")
              val all = st.map{f => src""" "${f._1}: " + std::to_string(${ptr(f._2.tp)}${toApproxFix(src"${f._1}", f._2.tp)})"""}.mkString("+ \", \" + ")
              emit(src"return $all;")
            close("}")
            st.foreach{f => emit(src"void set${f._1}(${f._2.tp} x){ this->${f._1} = ${amp(f._2.tp)}${toTrueFix("x",f._2.tp)}; }")}

          try {
            val rawtp = asIntType(lhs.tp)
            var position = 0
            open(src"$rawtp toRaw() { /* For compacting struct into one int */")
              emit(src"$rawtp result = 0;")
              st.foreach{f => 
                val field = f._1
                val t = f._2
                emit(src"result = result | (($rawtp) (this->$field) << $position); ")
                position = position + bitWidth(t.tp)
              }
              emit(src"return result;")
            close("}")
            position = 0
            open(src"${struct}(int128_t bits){ /* Constructor from raw bits */")
              st.foreach{f => emit(src"set${f._1}((${f._2.tp}) (bits >> $position));"); position = position + bitWidth(f._2.tp)}
            close("}")
            close(" ")
          } catch { case _:Throwable => }

          close("} __attribute__((packed));")
          // emit(src"typedef $struct ${lhs.tp};")

        }
      }
      val fields = st.zipWithIndex.map{case (f,i) => src"${f._2}"}
      emit(src"${struct} $lhs = ${struct}(${fields.mkString(",")});")

    case FieldApply(struct, field) => 
      if (isArrayType(lhs.tp)) emit(src"""${lhs.tp}* $lhs = ${struct}.$field;""")
      else emit(src"""${lhs.tp} $lhs = ${struct}.$field;""")



    case VecAlloc(elems)     => emit(src"${lhs.tp} $lhs{$elems};")
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

    case op @ MapIndices(size, func)   =>
      val isVoid = op.A.isVoid
      if (!isVoid) emitNewArray(lhs, lhs.tp, src"$size")
      open(src"for (int ${func.input} = 0; ${func.input} < $size; ${func.input}++) {")
      visitBlock(func)
      if (isArrayType(func.result.tp)) {
        if (getNestingLevel(func.result.tp) > 1) {Console.println(s"ERROR: Need to fix more than 2D arrays")}
        emit(src"(*$lhs)[${func.input}].resize(${getSize(func.result)});")
        open(src"for (int ${func.result}_copier = 0; ${func.result}_copier < ${getSize(func.result)}; ${func.result}_copier++) {")
          emit(src"(*$lhs)[${func.input}][${func.result}_copier] = (*${func.result})[${func.result}_copier];")
        close("}")
      } else {
        if (!isVoid) emit(src"(*$lhs)[${func.input}] = ${func.result};")
      }
      close("}")

    case SeriesForeach(start,end,step,func) =>
      open(src"for (int ${func.input} = $start; ${func.input} < ${end}; ${func.input} = ${func.input} + $step) {")
        visitBlock(func)
      close("}")

    case ArrayMkString(array,start,delim,end) =>
      emit(src"""${lhs.tp} $lhs = $start;""")
      open(src"for (int ${lhs}_i = 0; ${lhs}_i < (*${array}).size(); ${lhs}_i++){ ")
        emit(src"${lhs} = ((${lhs}, ${delim}) + std::to_string((*${array})[${lhs}_i]));")
      close("}")
      emit(src"""$lhs = ($lhs + $end);""")

    case op @ IfThenElse(cond, thenp, elsep) =>
      val star = lhs.tp match {case _: host.Array[_] => "*"; case _ => ""}
      if (!op.R.isVoid) emit(src"${lhs.tp}${star} $lhs;")
      open(src"if ($cond) { ")
        visitBlock(thenp)
        if (!op.R.isVoid) emit(src"${lhs} = ${thenp.result};")
      close("}")
      open("else {")
        visitBlock(elsep)
        if (!op.R.isVoid) emit(src"${lhs} = ${elsep.result};")
      close("}")

    case ArrayFilter(array, apply, cond) =>
      emit(src"${lhs.tp}* $lhs = new ${lhs.tp};")
      open(src"for (int ${apply.inputB} = 0; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) { ")
      visitBlock(apply)
      visitBlock(cond)
      emit(src"if (${cond.result}) (*${lhs}).push_back(${apply.result});")
      close("}")

    case ArrayFlatMap(array, apply, func) =>
      emit(src"${lhs.tp}* $lhs = new ${lhs.tp};")
      open(src"for (int ${apply.inputB} = 0; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) { ")
        visitBlock(apply)
        visitBlock(func)
        emit(src"for (int ${func.result}_idx = 0; ${func.result}_idx < (*${func.result}).size(); ${func.result}_idx++) {(*${lhs}).push_back((*${func.result})[${func.result}_idx]);}")
      close("}")

    case op@ArrayFromSeq(seq)   => 
      emitNewArray(lhs, lhs.tp, s"${seq.length}")
      seq.zipWithIndex.foreach{case (s,i) => 
        emit(src"(*${lhs})[$i] = $s;")
      }

    case ArrayForeach(array,apply,func) =>
      open(src"for (int ${apply.inputB} = 0; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) {")
      visitBlock(apply)
      visitBlock(func)
      close("}")

    case ArrayMap(array,apply,func) =>
      emitNewArray(lhs, lhs.tp, getSize(array))
      open(src"for (int ${apply.inputB} = 0; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) { ")
      visitBlock(apply)
      visitBlock(func)
      emitUpdate(lhs, func.result, src"${apply.inputB}", func.result.tp)
      close("}")
      

    case ArrayZip(a, b, applyA, applyB, func) =>
      emitNewArray(lhs, lhs.tp, getSize(a))
      open(src"for (int ${applyA.inputB} = 0; ${applyA.inputB} < ${getSize(a)}; ${applyA.inputB}++) { ")
      visitBlock(applyA)
      visitBlock(applyB)
      visitBlock(func)
      emitUpdate(lhs, func.result, src"${applyA.inputB}", func.result.tp)
      close("}")

    case ArrayUpdate(arr, id, data) => emitUpdate(arr, data, src"${id}", data.tp)

    case UnrolledForeach(ens,cchain,func,iters,valids,_) if (!inHw) => 
      val starts = cchain.counters.map(_.start)
      val ends = cchain.counters.map(_.end)
      val steps = cchain.counters.map(_.step)
      iters.zipWithIndex.foreach{case (i,idx) => 
        open(src"for (int $i = ${starts(idx)}; $i < ${ends(idx)}; $i = $i + ${steps(idx)}) {")
        valids(idx).foreach{v => emit(src"${v.tp} ${v} = true; // TODO: Safe to assume this in cppgen?")}
      }
      visitBlock(func)
      iters.foreach{_ => close("}")}

    case ArrayFold(array,init,apply,reduce) => 
      if (isArrayType(lhs.tp)) {
        throw new Exception(s"Codegen for ArrayFold onto an Array needs to be implemented!")
      }
      emit(src"${lhs.tp} $lhs = $init;")

      open(src"for (int ${apply.inputB} = 0; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) {")
        emitApply(reduce.inputA, array, src"${apply.inputB}")
        emit(src"""${reduce.inputB.tp}${if (isArrayType(reduce.inputB.tp)) "*" else ""} ${reduce.inputB} = $lhs;""")
        visitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("}")

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
        emit(src"$lhs = ${zeroElement(lhs.tp)};")
      close("}")

      open(src"for (int ${apply.inputB} = 1; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) {")
        emitApply(reduce.inputA, array, src"${apply.inputB}")
        emit(src"""${reduce.inputB.tp}${if (isArrayType(reduce.inputB.tp)) "*" else ""} ${reduce.inputB} = $lhs;""")
        visitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("}")

    //case ArrayGroupByReduce(array, apply, key, map, reduce) =>
      //if (isArrayType(lhs.tp)) {
        //emit(src"""${lhs.tp}* $lhs = new ${lhs.tp}(${getSize(array, "[0]")});""") 
      //} else {
        //emit(src"${lhs.tp} $lhs;") 
      //}
      //open(src"if (${getSize(array)} > 0) { // Hack to handle reductions on things of length 0")
        //emitApply(lhs, array, "0", false)
      //close("}")
      //open("else {")
        //emit(src"$lhs = ${zeroElement(lhs.tp)};")
      //close("}")

      //open(src"for (int ${apply.inputB} = 1; ${apply.inputB} < ${getSize(array)}; ${apply.inputB}++) {")
        //emitApply(key.input, array, src"${apply.inputB}")
        //visitBlock(key)
        //visitBlock(map)
        //emit(src"""${reduce.inputB.tp}${if (isArrayType(reduce.inputB.tp)) "*" else ""} ${reduce.inputB} = $lhs;""")
        //visitBlock(reduce)
        //emit(src"$lhs = ${reduce.result};")
      //close("}")

    case op@Switch(selects,block) if !inHw =>

      emit(src"/** BEGIN SWITCH $lhs **/")
      if (op.R.isBits) emit(src"${lhs.tp} $lhs;")
      selects.indices.foreach { i =>
        open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) {""")
          visitBlock(op.cases(i).body)
          if (op.R.isBits) emit(src"${lhs} = ${op.cases(i).body.result};")
        close("}")
      }
      emit(src"/** END SWITCH $lhs **/")

    case SwitchCase(body) if !inHw => // Controlled by Switch

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

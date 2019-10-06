package spatial.codegen.surfgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._

trait SurfGenArray extends SurfGenCommon {

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
    case InputArguments()       => emit(src"$lhs = cliargs;")

    case ArrayApply(array @ Def(InputArguments()), i) => emit(src"$lhs = $array[$i]")

    case ArrayApply(array, i)   => emit(src"$lhs = $array[$i]")
    case op@ArrayNew(size)      => emit(src"$lhs = [None for _ in range($size)]")
    case ArrayLength(array)     => emit(src"$lhs = len($array)")
    case DataAsBits(bits)       => emit(src"$lhs = bin($bits)[2:]")
    case BitsAsData(v,mT) => emit(src"$lhs = int($v, 2)")
    case DataAsVec(bits)       => emit(src"$lhs = $bits # TBD")
    case VecAsData(v,mT) => emit(src"$lhs = $v # TBD")
    case SimpleStruct(st) => emit(src"""$lhs = struct.pack("B"*10 ,$st) # TBD""")
    case FieldApply(struct, field) => emit(src"$lhs = $struct.get($field) # TBD")
    case VecAlloc(elems)     => emit(src"$lhs = [${elems.mkString(",")}]")
    case VecApply(vector, i) => emit(src"$lhs = $vector[$i];")
    case VecSlice(vector, start, end) => emit(src"$lhs = $vector[$start:$end]")
    case VecConcat(elems) => emit(src"$lhs = $lhs + $elems")
    case op @ MapIndices(size, func)   =>
      val isVoid = op.A.isVoid
      if (!isVoid) emitNewArray(lhs, lhs.tp, src"$size")
      open(src"for ${func.input} in range(0,$size):")
      visitBlock(func)
      if (!isVoid) emit(src"$lhs[${func.input}] = ${func.result}")
      close("")

    case SeriesForeach(start,end,step,func) =>
      open(src"for ${func.input} in range($start,$end,$step):")
        visitBlock(func)
      close("")

    case ArrayMkString(array,start,delim,end) =>
      emit(src"""$lhs = $start;""")
      open(src"for ${lhs}_i in range(0,len(${array})):")
        emit(src"${lhs} = ${lhs} + ${delim} + str(${array}[${lhs}_i]);")
      close("")
      emit(src"""$lhs = $lhs + $end;""")


    case ArrayMap(array,apply,func) =>
      emit(src"$lhs = [None for _ in range(${getSize(array)})]")
      open(src"for ${apply.inputB} in range(0,${getSize(array)}):")
      visitBlock(apply)
      visitBlock(func)
      emit(src"${lhs}[${apply.inputB}] = ${func.result}")
      close("")

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
      emit(src"$lhs = $init;")

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

    case _ => super.gen(lhs, rhs)
  }


}

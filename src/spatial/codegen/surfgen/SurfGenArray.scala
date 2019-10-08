package spatial.codegen.surfgen

import argon._
import argon.node._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import spatial.metadata.types._

trait SurfGenArray extends SurfGenCommon {

  var struct_list: List[String] = List()

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
      if (!isVoid) emit(src"$lhs = [None for _ in range($size)]")
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
      emit(src"$lhs = [None for _ in range(len(${array}))]")
      open(src"for ${apply.inputB} in range(0,len(${array})):")
      visitBlock(apply)
      visitBlock(func)
      emit(src"$lhs[${apply.inputB}] = ${func.result}")
      close("")

    case op @ IfThenElse(cond, thenp, elsep) =>
      open(src"if ($cond): ")
        visitBlock(thenp)
        if (!op.R.isVoid) emit(src"$lhs = ${thenp.result}")
      closeopen("else:")
        visitBlock(elsep)
        if (!op.R.isVoid) emit(src"$lhs = ${elsep.result}")
      close("")

    case ArrayFilter(array, apply, cond) =>
      emit(src"$lhs = []")
      open(src"for ${apply.inputB} in range(len(${array}):")
      visitBlock(apply)
      visitBlock(cond)
      emit(src"if (${cond.result}): $lhs.append(${apply.result})")
      close("")

    case ArrayFlatMap(array, apply, func) =>
      open(src"for ${apply.inputB} in range(len(${array})):")
        visitBlock(apply)
        visitBlock(func)
        emit(src"for ${func.result}_idx in range(len(${func.result})): $lhs.append(${func.result}[${func.result}_idx])")
      close("")

    case op@ArrayFromSeq(seq)   =>
      emit(src"$lhs = [${seq.mkString(",")}]")

    case ArrayForeach(array,apply,func) =>
      open(src"for ${apply.inputB} in range(len(${array})):")
      visitBlock(apply)
      visitBlock(func)
      close("")

    case ArrayZip(a, b, applyA, applyB, func) =>
      emit(src"$lhs = [None for _ in range(len(${a}))]")
      open(src"for ${applyA.inputB} in range(len(${a})):")
      visitBlock(applyA)
      visitBlock(applyB)
      visitBlock(func)
      emit(src"$lhs[${applyA.inputB}] = ${func.result}")
      close("")

    case ArrayUpdate(arr, id, data) => emit(src"${arr}[$id] = $data")

    case UnrolledForeach(ens,cchain,func,iters,valids,_) if (!inHw) =>
      emit(s"raise Exception('no implementation for UnrolledForeach?!?')")

    case ArrayFold(array,init,apply,reduce) =>
      if (isArrayType(lhs.tp)) {
        throw new Exception(s"Codegen for ArrayFold onto an Array needs to be implemented!")
      }
      emit(src"$lhs = $init;")

      open(src"for ${apply.inputB} in range(len($array)):")
        emit(src"${reduce.inputA} = $array[${apply.inputB}]")
        emit(src"""${reduce.inputB} = $lhs;""")
        visitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("")

    case ArrayReduce(array, apply, reduce) =>
      if (isArrayType(lhs.tp)) {
        emit(src"""$lhs = [None for _ in range(len($array))]""")
      } else {
        emit(src"$lhs = 0")
      }
      open("if (len(%s) > 0): # Hack to handle reductions on things of length 0".format(array))
        emit(src"$lhs = $array[0]")
      closeopen("else:")
        emit(src"$lhs = 0")
      close("")

      open(src"for ${apply.inputB} in range(1,len($array)):")
        emit(src"${reduce.inputA} = $array[${apply.inputB}]")
        emit(src"""${reduce.inputB} = $lhs;""")
        visitBlock(reduce)
        emit(src"$lhs = ${reduce.result};")
      close("")

    case op@Switch(selects,block) if !inHw =>

      emit(src"## BEGIN SWITCH $lhs")
      selects.indices.foreach { i =>
        open(src"""${if (i == 0) "if" else "else if"} (${selects(i)}):""")
          visitBlock(op.cases(i).body)
          if (op.R.isBits) emit(src"${lhs} = ${op.cases(i).body.result};")
        close("}")
      }
      emit(src"/** END SWITCH $lhs **/")

    case SwitchCase(body) if !inHw => // Controlled by Switch

    case _ => super.gen(lhs, rhs)
  }


}

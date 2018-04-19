package spatial.codegen.scalagen

import argon._
import spatial.data._
import spatial.lang._
import spatial.node._

trait ScalaGenArray extends ScalaCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: Tensor1[_] => src"Array[${tp.A}]"
    case _ => super.remap(tp)
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArrayNew(size)      => emit(src"val $lhs = new ${op.R}($size)")
    case op@ArrayFromSeq(seq)   => emit(src"val $lhs = ${op.R}($seq)")

    case ArrayApply(array @ Op(InputArguments()), i) =>

      emit(src"// Commandline argument #$i (${lhs.name.getOrElse("<unnamed>")})")
      emit(src"val $lhs = try{ $array.apply($i) }")
      open(src"catch {case _:Throwable =>")
        CLIArgs.get(i) match {
          case Some(name) => emit(src"""println("Missing argument " + $i + " ('$name')")""")
          case _          => emit(src"""println("Missing argument " + $i)""")
        }
        emit(src"printHelp()")
        emit(src"sys.exit(-1)")
      close("}")


    case ArrayApply(array, i)   => emit(src"val $lhs = $array.apply($i)")
    case ArrayLength(array)     => emit(src"val $lhs = $array.length")
    case InputArguments()       => 
      emit(src"val $lhs = args")
      emit(src"""if (args.contains("--help") || args.contains("-h")) {printHelp()}""")

    case ArrayUpdate(array, i, data) => emit(src"val $lhs = $array.update($i, $data)")
    case MapIndices(size, func) =>
      open(src"val $lhs = Array.tabulate($size){bbb => ")
        emit(src"val ${func.input} = FixedPoint.fromInt(bbb)")
        ret(func)
      close("}")

    case ArrayForeach(array,apply,func) =>
      open(src"val $lhs = $array.indices.foreach{bbb => ")
        emit(src"val ${apply.inputB} = FixedPoint.fromInt(bbb)")
        gen(apply)
        ret(func)
      close("}")

    case ArrayMap(array,apply,func) =>
      open(src"val $lhs = Array.tabulate($array.length){bbb => ")
        emit(src"val ${apply.inputB} = FixedPoint.fromInt(bbb)")
        gen(apply)
        ret(func)
      close("}")

    case ArrayZip(a, b, applyA, applyB, func) =>
      open(src"val $lhs = Array.tabulate($a.length){bbb => ")
        emit(src"val ${applyA.inputB} = FixedPoint.fromInt(bbb)")
        gen(applyA)
        gen(applyB)
        ret(func)
      close("}")

    case ArrayReduce(array, _, reduce) =>
      open(src"val $lhs = $array.reduce{(${reduce.inputA},${reduce.inputB}) => ")
        ret(reduce)
      close("}")

    case ArrayFilter(array, _, cond) =>
      open(src"val $lhs = $array.filter{${cond.input} => ")
        ret(cond)
      close("}")

    case ArrayFlatMap(array, _, func) =>
      open(src"val $lhs = $array.flatMap{${func.input} => ")
        ret(func)
      close("}")

    case ArrayMkString(array,start,delim,end) =>
      emit(src"val $lhs = $array.mkString($start, $delim, $end)")

    case _ => super.gen(lhs, rhs)
  }
}

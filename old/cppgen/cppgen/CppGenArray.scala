package argon.codegen.cppgen

import argon.core._
import argon.nodes._
import argon.emul.FixedPoint

trait CppGenArray extends CppCodegen {
  protected def isArrayType(tp: Type[_]): Boolean = tp match {
    case tp: ArrayType[_] => tp.typeArguments.head match {
      case tp: ArrayType[_] => println("EXCEPTION: Probably can't handle nested array types in ifthenelse"); true
      case _ => true
    }
    case _ => false
  }

  protected def getSize(array: Exp[_], extractor:String = ""): String = {
    src"(*${array})${extractor}.size()"
  }

  protected def emitNewArray(lhs: Exp[_], tp: Type[_], size: String): Unit = {
    emit(src"${tp}* $lhs = new ${tp}($size);")
  }

  protected def emitApply(dst: Exp[_], array: Exp[_], i: String, isDef: Boolean = true): Unit = {
    // val get = if (src"${array.tp}" == "cppDeliteArraystring") {
    //   if (isDef) {
    //     emit(src"${dst.tp} $dst = ${array}->apply($i);")  
    //   } else {
    //     emit(src"$dst = ${array}->apply($i);")  
    //   }
    // } else {
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

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: ArrayType[_] => src"vector<${tp.typeArguments.head}>"
    case _ => super.remap(tp)
  }

  override protected def quoteConst(c: Const[_]): String = (c.tp, c) match {
    // Array constants are currently disallowed
    case _ => super.quoteConst(c)
  }

  override protected def emitNode(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case op@ArrayNew(size)      => emitNewArray(lhs, lhs.tp, src"$size")
    case op@ArrayFromSeq(seq)   =>
      // TODO: Not sure if this is right
      emit(src"""${lhs.tp.typeArguments.head} ${lhs}_vs[${seq.length}] = {${seq.map(quote).mkString(",")}};""")
      emit(src"""${lhs.tp} ${lhs}_v(${lhs}_vs, ${lhs}_vs + ${seq.length});""")
      emit(src"${lhs.tp}* $lhs = &${lhs}_v;")

    case ArrayApply(array, i)   => 
      emitApply(lhs, array, src"$i")
      array match {
        case Def(InputArguments()) => 
          if (lhs.name.isDefined) {
            val ii = i match {case c: Const[_] => c match {case Const(c: FixedPoint) => c.toInt; case _ => -1}; case _ => -1}
            if (cliArgs.contains(ii)) cliArgs += (ii -> s"${cliArgs(ii)} / ${lhs.name.get}")
            else cliArgs += (ii -> lhs.name.get)
          }
        case _ =>
      }

    case ArrayLength(array)     => emit(src"${lhs.tp} $lhs = ${getSize(array)};")
    case InputArguments()       => emit(src"${lhs.tp}* $lhs = args;")
    case _ => super.emitNode(lhs, rhs)
  }
}

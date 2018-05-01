package spatial.codegen.cppgen

import argon._
import argon.codegen.Codegen
import spatial.lang._
import spatial.node._
import emul.FloatPoint
import emul.FixedPoint
import utils.escapeString
import spatial.util._
import emul.Bool



trait CppGenCommon extends CppCodegen { 

  var controllerStack = scala.collection.mutable.Stack[Sym[_]]()
  var argOuts = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIOs = scala.collection.mutable.HashMap[Sym[_], Int]()
  var argIns = scala.collection.mutable.HashMap[Sym[_], Int]()
  var drams = scala.collection.mutable.HashMap[Sym[_], Int]()

  /* Represent a FixPt with nonzero number of f bits as a bit-shifted int */
  protected def toTrueFix(x: String, tp: Type[_]): String = {
    tp match {
      case FixPtType(s,d,f) if (f != 0) => src"(${asIntType(tp)}) ($x * ((${asIntType(tp)})1 << $f))"
      case _ => src"$x"
    }
  }
  /* Represent a FixPt with nonzero number of f bits as a float */
  protected def toApproxFix(x: String, tp: Type[_]): String = {
    tp match {
      case FixPtType(s,d,f) if (f != 0) => src"(${tp}) ($x / ((${asIntType(tp)})1 << $f))"
      case _ => src"$x"
    }
  }

  protected def asIntType(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => 
       if (d+f > 64) s"int128_t"
       else if (d+f > 32) s"int64_t"
       else if (d+f > 16) s"int32_t"
       else if (d+f > 8) s"int16_t"
       else if (d+f > 4) s"int8_t"
       else if (d+f > 2) s"int8_t"
       else if (d+f == 2) s"int8_t"
       else "bool"
    case FltPtType(m,e) => "float"
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) => 
      val u = if (!s) "u" else ""
      if (f > 0) {"double"} else {
        if (d+f > 64) s"${u}int128_t"
        else if (d+f > 32) s"${u}int64_t"
        else if (d+f > 16) s"${u}int32_t"
        else if (d+f > 8) s"${u}int16_t"
        else if (d+f > 4) s"${u}int8_t"
        else if (d+f > 2) s"${u}int8_t"
        else if (d+f == 2) s"${u}int8_t"
        else "bool"
      }
    case FloatType()  => "float"
    case DoubleType() => "double"
    case _: Bit => "bool"
    case _: Text => "string"
    case ai: Reg[_] => remap(ai.typeArgs.head)
    case _: Vec[_] => "vector<" + remap(tp.typeArgs.head) + ">"
    case _ => 
      tp.typePrefix match {
        case "Array" => "vector<" + remap(tp.typeArgs.head) + ">"
        case _ => super.remap(tp)
      }
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(s,d,f), _) => c.toString + {if (f+d > 32) "L" else ""}
    case (FltPtType(g,e), _) => c.toString
    case (_:Text, cc: String) => "string(" + escapeString(cc) + ")"
    case (_:Bit, c:Bool) => s"${c.value}"
    case _ => super.quoteConst(tp,c)
  }

  var argHandleMap = scala.collection.mutable.HashMap[Sym[_], String]() // Map for tracking defs of nodes and if they get redeffed anywhere, we map it to a suffix
  def argHandle(d: Sym[_]): String = {
    if (argHandleMap.contains(d)) {
      argHandleMap(d)
    } else {
      val attempted_name = d.name.getOrElse(quote(d)).toUpperCase
      if (argHandleMap.values.toList.contains(attempted_name)) {
        val taken = argHandleMap.values.toList.filter(_.contains(attempted_name))
        val given_name = attempted_name + "_dup" + taken.length
        argHandleMap += (d -> given_name)
        given_name
      } else {
        argHandleMap += (d -> attempted_name)
        attempted_name
      }
    }
  }


  protected def fracBits(tp: Type[_]) = tp match {case FixPtType(s,d,f) => f; case _ => 0}

  protected def bitWidth(tp: Type[_]): Int = tp match {
    case FixPtType(s,d,f) => d+f; 
    case FltPtType(g,e) => g+e; 
    case BitType() => 1
    case t: Vec[_] => t.width * bitWidth(t.typeArgs.head)}

}
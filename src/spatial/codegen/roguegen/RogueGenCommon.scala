package spatial.codegen.roguegen

import argon._
import spatial.lang._
import spatial.node._
import spatial.metadata.control._
import utils.escapeString
import emul.Bool
import spatial.util.spatialConfig

trait RogueGenCommon extends RogueCodegen {

  var instrumentCounters: List[(Sym[_], Int)] = List()
  var earlyExits: List[Sym[_]] = List()

  protected def instrumentCounterIndex(s: Sym[_]): Int = {
    if (spatialConfig.enableInstrumentation) {
      instrumentCounters.takeWhile(_._1 != s).map{x =>
        2 + {if (hasBackPressure(x._1.toCtrl) || hasForwardPressure(x._1.toCtrl)) 2 else 0}
      }.sum
    } else 0
  }
  protected def instrumentCounterArgs(): Int = {
    if (spatialConfig.enableInstrumentation) {
      val last = instrumentCounters.last._1
      instrumentCounterIndex(last) + 2 + {if (hasBackPressure(last.toCtrl) || hasForwardPressure(last.toCtrl)) 2 else 0}
    } else 0
  }

  var controllerStack = scala.collection.mutable.Stack[Sym[_]]()
  var argOuts = scala.collection.mutable.ArrayBuffer[Sym[_]]()
  var argIOs = scala.collection.mutable.ArrayBuffer[Sym[_]]()
  var argIns = scala.collection.mutable.ArrayBuffer[Sym[_]]()
  var frames = scala.collection.mutable.ArrayBuffer[Sym[_]]()

  def hasForwardPressure(sym: Ctrl): Boolean = sym.hasStreamAncestor && getReadStreams(sym).nonEmpty
  def hasBackPressure(sym: Ctrl): Boolean = sym.hasStreamAncestor && getWriteStreams(sym).nonEmpty

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
      case FixPtType(s,d,f) if (f != 0) => src"(${tp}) ((${tp}) $x / ((${asIntType(tp)})1 << $f))"
      case _ => src"$x"
    }
  }

  protected def asIntType(tp: Type[_]): String = tp match {
    case _ => ""
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s,d,f) =>
      val u = if (!s) "u" else ""
      if (f == 0) {
        if (d > 64) s"${u}int128"
        else if (d > 32) s"${u}int64"
        else if (d > 16) s"${u}int32"
        else if (d > 8) s"${u}int16"
        else if (d > 4) s"${u}int8"
        else if (d > 2) s"${u}int8"
        else if (d == 2) s"${u}int8"
        else "bool"
      }
      else { "double" } //s"numeric::Fixed<$d, $f>"
    case FloatType()  => "float"
    case DoubleType() => "double"
    case FltPtType(g,e) => "float"
    case _: Bit => "bool"
    case _: Text => "string"
//    case ai: Reg[_] => remap(ai.typeArgs.head)
//    case _: Vec[_] => "vector<" + remap(tp.typeArgs.head) + ">"
//    case t: Tup2[_,_] => s"${super.remap(tp)}".replaceAll("\\[","").replaceAll("\\]","").replaceAll(",","")
//    case _: host.Array[_] => "vector<" + remap(tp.typeArgs.head) + ">"
    case _ => super.remap(tp)
  }

  protected def conv(tp: Type[_]): String = tp match {
    case _ => ""
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = (tp,c) match {
    case (FixPtType(s,d,f), _) => c.toString.replace("Const(","").replace(")","")
    case (FltPtType(g,e), _) => c.toString.replace("Const(","").replace(")","")
    case (_:Text, cc: String) => "str(" + escapeString(cc) + ")"
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
    case Bits(bT) => bT.nbits
    case _ => -1
  }

}

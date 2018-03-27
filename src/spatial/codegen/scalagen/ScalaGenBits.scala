package spatial.codegen.scalagen

import argon._
import spatial.lang._
import spatial.node._

trait ScalaGenBits extends ScalaCodegen {

  def fixConst(c: Int, tp: Type[_]): String = tp match {
    case FixPtType(s,i,f) => s"""FixedPoint(BigDecimal("$c"), FixFormat($s,$i,$f))"""
    case _ => throw new Exception("Cannot create float constant of non-float type")
  }
  def fltConst(c: Int, tp: Type[_]): String = tp match {
    case FltPtType(g,e) => s"""FloatPoint(BigDecimal("$c"), FltFormat(${g-1},$e))"""
    case _ => throw new Exception("Cannot create float constant of non-float type")
  }

  def one(tp: ExpType[_,_]): String = tp match {
    case _:Flt[_,_]   => fltConst(1,tp)
    case _:Fix[_,_,_] => fixConst(1,tp)
  }

  dependencies ::= FileDep("scalagen", "Bool.scala")
  dependencies ::= FileDep("scalagen", "FixedPoint.scala")
  dependencies ::= FileDep("scalagen", "FixedPointRange.scala")
  dependencies ::= FileDep("scalagen", "FloatPoint.scala")
  dependencies ::= FileDep("scalagen", "DataImplicits.scala")
  dependencies ::= FileDep("scalagen", "Number.scala")

  def invalid(tp: Type[_]): String = tp match {
    case _ => throw new Exception(s"Don't know how to generate invalid for type $tp")
  }

  override def emitHeader(): Unit = {
    emit(src"import DataImplicits._")
    super.emitHeader()
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case Mux(sel, a, b) => emit(src"val $lhs = if ($sel) $a else $b")
    case op @ OneHotMux(selects,datas) =>
      open(src"val $lhs = {")
      selects.indices.foreach { i =>
        emit(src"""${if (i == 0) "if" else "else if"} (${selects(i)}) { ${datas(i)} }""")
      }
      emit(src"else { ${invalid(op.R)} }")
      close("}")

    case e@DataAsBits(a) => a.tp match {
      case FltPtType(_,_)   => emit(src"val $lhs = $a.bits")
      case FixPtType(_,_,_) => emit(src"val $lhs = $a.bits")
      case BitType()        => emit(src"val $lhs = Array[Bool]($a)")
    }

    case BitsAsData(v,a) => a match {
      case FltPtType(g,e)   => emit(src"val $lhs = FloatPoint.fromBits($v, FltFormat(${g-1},$e))")
      case FixPtType(s,i,f) => emit(src"val $lhs = FixedPoint.fromBits($v, FixFormat($s,$i,$f))")
      case BitType()        => emit(src"val $lhs = $v.head")
    }

    case _ => super.gen(lhs,rhs)
  }
}

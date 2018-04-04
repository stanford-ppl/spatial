package spatial.codegen.cppgen

import argon.core._
import argon.nodes._
import argon.codegen.cppgen.CppCodegen
import spatial.aliases._
import spatial.nodes._


trait CppGenSRAM extends CppCodegen {

  override protected def remap(tp: Type[_]): String = tp match {
    case tp: SRAMType[_] => src"Array[${tp.child}]"
    case _ => super.remap(tp)
  }

  override protected def name(s: Dyn[_]): String = s match {
    case Def(SRAMNew(_)) => s"${s}_sram"
    case _ => super.name(s)
  }

  var argHandleMap = scala.collection.mutable.HashMap[Exp[_], String]() // Map for tracking defs of nodes and if they get redeffed anywhere, we map it to a suffix
  def argHandle(d: Exp[_]): String = {
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

  protected def remapIntType(tp: Type[_]): String = tp match {
    case IntType() => "int32_t"
    case LongType() => "int32_t"
    case FixPtType(s,d,f) => 
      if (d+f > 64) "int128_t"
      else if (d+f > 32) "int64_t"
      else if (d+f > 16) "int32_t"
      else if (d+f > 8) "int16_t"
      else if (d+f > 4) "int8_t"
      else if (d+f > 2) "int2_t"
      else "boolean"
    case FltPtType(e,m) => 
      if (e+m == 32) "float"
      else if (e+m == 64) "double"
      else if (e+m == 16) "half"
      else "faulty_float"
    case _ => "notype"
  }

  def flattenAddress(dims: Seq[Exp[Index]], indices: Seq[Exp[Index]], ofs: Option[Exp[Index]]): String = {
    val strides = List.tabulate(dims.length){i => (dims.drop(i+1).map(quote) :+ "1").mkString("*") }
    indices.zip(strides).map{case (i,s) => src"$i*$s" }.mkString(" + ") + ofs.map{o => src" + $o"}.getOrElse("")
  }

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = rhs match {
    case _ => super.gen(lhs, rhs)
  }
}

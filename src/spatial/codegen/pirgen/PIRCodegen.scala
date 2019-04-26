package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata.CLIArgs
import spatial.metadata.memory._
import spatial.metadata.bounds._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable
import spatial.traversal.AccelTraversal
import spatial.codegen.scalagen.ScalaCodegen
import scala.language.reflectiveCalls
import spatial.node._

trait PIRCodegen extends Codegen with FileDependencies with AccelTraversal with PIRFormatGen with PIRGenHelper { self =>
  override val lang: String = "pir"
  override val ext: String = "scala"
  backend = "accel"

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  private var globalBlockID: Int = 0

  override def entryFile: String = s"AccelMain.$ext"

  final override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    rhs match {
      case AccelScope(func) => inAccel { genAccel(lhs, rhs) }
      case ArgInNew(init) => genAccel(lhs, rhs)
      case ArgOutNew(init) => genAccel(lhs, rhs)
      case StreamInNew(init) => genAccel(lhs, rhs)
      case StreamOutNew(init) => genAccel(lhs, rhs)
      case DRAMHostNew(dims,zero) => genAccel(lhs, rhs)
      case rhs if (inHw) => genAccel(lhs, rhs)
      case rhs => rhs.blocks.foreach(ret)
    }
  }

  final override def emitHeader(): Unit = {
    super.emitHeader()
    emitAccelHeader
  }

  final override def emitFooter():Unit = {
    emitAccelFooter
    super.emitFooter()
  }

  def emitHostHeader = {
    open(src"object Main {")
      open(src"def main(args: Array[String]): Unit = {")
  }

  def emitHostFooter = {
      close(s"""}""")
    close(s"""}""")
  }

  def emitAccelHeader = {
    emit("import pir._")
    emit("import pir.node._")
    emit("import spade.param._")
    emit("import prism.graph._")
    emit("")
    open(s"""object AccelMain extends PIRApp {""")
    open(src"def staging(top:Top) = {")
    emit("""import top._""")
  }

  def emitAccelFooter = {
    close("}")
    close("}")
  }

  override def quote(s: Sym[_]): String = s match {
    case VecConst(vs) => src"Const(${vs}).tp(${s.tp})"
    case s => super.quote(s)
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = c match {
    case c:String => s"""Const("${c.replace("\n","\\n")}")"""
    case c => src"Const($c).tp(${tp})"
  }

  override protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Set[_]   => 
      s"Set(${p.map(quoteOrRemap).mkString(", ")})" 
    case p: Iterable[_]   => 
      s"List(${p.map(quoteOrRemap).mkString(", ")})" 
    case e: Ref[_,_]   => quote(e)
    case Lhs(sym, None) => s"${quote(sym)}"
    case Lhs(sym, Some(post)) => s"${quote(sym)}_$post"
    case l: Long       => l.toString + "L"
    case None    => "None"
    case Some(x) => "Some(" + quoteOrRemap(x) + ")"
    case x => x.toString
  }

  override protected def remap(tp: Type[_]): String = tp match {
    case FixPtType(s, i, f) => src"Fix(${s}, ${i}, ${f})"
    case FltPtType(m, e) => src"Flt(${m}, ${e})"
    case _:Bit => src"Bool"
    case tp:Vec[_] => remap(tp.A) //TODO
    //case tp:Vec[_] => src"Vec(${tp.A}, ${tp.width})"
    case tp => super.remap(tp)
  }

  protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = {
    emit(s"// $lhs = $rhs TODO: Unmatched Node")
    rhs.blocks.foreach(ret)
  }

  override protected def emitEntry(block: Block[_]): Unit = gen(block)

}

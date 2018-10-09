package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata.CLIArgs
import spatial.metadata.memory._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable
import spatial.traversal.AccelTraversal

trait PIRCodegen extends Codegen with FileDependencies with AccelTraversal with PIRFormatGen with PIRGenHelper {
  override val lang: String = "pir"
  override val ext: String = "scala"
  backend = "accel"

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  private var globalBlockID: Int = 0

  override def emitHeader(): Unit = {
    inGen(out, "AccelMain.scala") {
      emit("import pir._")
      emit("import pir.node._")
      emit("import arch._")
      emit("import prism.enums._")
      emit("")
      open(s"""object ${spatialConfig.name} extends PIRApp {""")
    }
    super.emitHeader()
  }

  override def emitFooter():Unit = {
    inGen(out, "AccelMain.scala") {
      emit(s"")
      close("}")
    }

    super.emitFooter()
  }

  override protected def quoteConst(tp: Type[_], c: Any): String = s"Const($c)"

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

  final override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = if (inHw) genHost(lhs, rhs) else genAccel(lhs, rhs)

  protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = {
    emit(s"// $lhs = $rhs TODO: Unmatched Node")
    rhs.blocks.foreach(ret)
  }

  protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = {
    emit(s"// $lhs = $rhs TODO: Unmatched Node")
    rhs.blocks.foreach(ret)
  }

  def emitPreMain(): Unit = { }
  def emitPostMain(): Unit = { }

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"object Host {")
      open(src"def main(args: Array[String]): Unit = {")
        emitPreMain()
        gen(block)
        emitPostMain()
      close(src"}")
    close(src"}")
  }

}

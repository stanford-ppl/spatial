package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata.CLIArgs
import spatial.metadata.memory._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable
import spatial.traversal.AccelTraversal
import spatial.codegen.scalagen.ScalaCodegen
import scala.language.reflectiveCalls

trait PIRCodegen extends Codegen with FileDependencies with AccelTraversal with PIRFormatGen with PIRGenHelper { self =>
  override val lang: String = "pir"
  override val ext: String = "scala"
  backend = "accel"

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  private var globalBlockID: Int = 0

  override def entryFile: String = s"Main.$ext"

  val hostGen = new spatial.codegen.scalagen.ScalaGenSpatial(IR) {
    override def out = self.out
    override protected def gen(block: Block[_], withReturn: Boolean = false): Unit = self.gen(block, withReturn)
    def genHost(lhs: Sym[_], rhs: Op[_]): Unit = gen(lhs, rhs)
  }

  final override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = if (inHw) genAccel(lhs, rhs) else genHost(lhs, rhs)

  val hostFile = "Main.scala.1"
  val accelFile = "AccelMain.scala"

  def openHost(blk: => Unit) = inGen(out, hostFile)(blk)

  def openAccel(blk: => Unit) = inGen(out, accelFile)(blk)

  final override def emitHeader(): Unit = {
    super.emitHeader()
    openHost { emitHostHeader }
    openAccel { emitAccelHeader }
  }

  final override def emitFooter():Unit = {
    openHost { emitHostFooter }
    openAccel { emitAccelFooter }
    super.emitFooter()
  }

  def emitHostHeader = {
    hostGen.emitHeader
    open(src"object Main {")
      open(src"def main(args: Array[String]): Unit = {")
  }

  def emitHostFooter = {
      close(s"""}""")
      hostGen.emitHelp
    close(s"""}""")
    hostGen.emitFooter
  }

  def emitAccelHeader = {
    emit("import pir._")
    emit("import pir.node._")
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

  protected def genHost(lhs: Sym[_], rhs: Op[_]): Unit = {
    hostGen.genHost(lhs, rhs)
  }

  protected def genAccel(lhs: Sym[_], rhs: Op[_]): Unit = {
    emit(s"// $lhs = $rhs TODO: Unmatched Node")
    rhs.blocks.foreach(ret)
  }

  final def genInAccel(lhs: Sym[_], rhs: Op[_]): Unit = openAccel { genAccel(lhs, rhs) }

  override protected def emitEntry(block: Block[_]): Unit = {
    openHost {
      gen(block)
    }
  }

}

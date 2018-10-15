package spatial.codegen.scalagen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.codegen.naming.NamedCodegen
import spatial.metadata.CLIArgs
import spatial.metadata.memory._
import spatial.lang._

import scala.collection.mutable

trait ScalaCodegen extends Codegen with FileDependencies with NamedCodegen {
  override val lang: String = "scala"
  override val ext: String = "scala"
  final val CODE_WINDOW: Int = 75

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "TRUE" else ens.map(quote).mkString(" & ")

  protected val scoped: mutable.Map[Sym[_],String] = new mutable.HashMap[Sym[_],String]()
  private var globalBlockID: Int = 0

  override def named(s: Sym[_], id: Int): String = {
    dbgs(s"Checking scoped for symbol $s: ${scoped.contains(s)}")
    scoped.getOrElse(s, super.named(s,id))
  }

  override def emitHeader(): Unit = {
    emit("import emul._")
    emit("import emul.implicits._")
    emit("")
    super.emitHeader()
  }

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    if (b.stms.length < CODE_WINDOW) {
      visitBlock(b)
    }
    else {
      globalBlockID += 1
      // TODO: More hierarchy? What if the block is > CODE_WINDOW * CODE_WINDOW size?
      val blockID: Int = globalBlockID
      var chunkID: Int = 0
      var chunk: Seq[Sym[_]] = Nil
      var remain: Seq[Sym[_]] = b.stms
      // TODO: Other ways to speed this up?
      // Memories are always global in scala generation right now
      def isLive(s: Sym[_]): Boolean = !s.isMem && (b.result == s || remain.exists(_.nestedInputs.contains(s)))
      while (remain.nonEmpty) {
        chunk = remain.take(CODE_WINDOW)
        remain = remain.drop(CODE_WINDOW)
        open(src"object block${blockID}Chunker$chunkID {")
          open(src"def gen(): Map[String, Any] = {")
          chunk.foreach{s => visit(s) }
          val live = chunk.filter(isLive)
          emit("Map[String,Any](" + live.map{s => src""""$s" -> $s""" }.mkString(", ") + ")")
          scoped ++= live.map{s => s -> src"""block${blockID}chunk$chunkID("$s").asInstanceOf[${s.tp}]"""}
          close("}")
        close("}")
        emit(src"val block${blockID}chunk$chunkID: Map[String, Any] = block${blockID}Chunker$chunkID.gen()")
        chunkID += 1
      }
    }
    if (withReturn) emit(src"${b.result}")
  }

  def emitPreMain(): Unit = { }
  def emitPostMain(): Unit = { }

  override protected def emitEntry(block: Block[_]): Unit = {
    open(src"object Main {")
      open(src"def main(args: Array[String]): Unit = {")
        emitPreMain()
        gen(block)
        emitPostMain()
      close(src"}")
      emitHelp
    close(src"}")
  }

  def emitHelp = {
    open("def printHelp(): Unit = {")
      val argsList = CLIArgs.listNames
      val examples: Iterator[Seq[String]] = if (argsList.nonEmpty) IR.runtimeArgs.grouped(argsList.size) else Iterator(Seq(""))
      emit(s"""System.out.print("Help for app: ${config.name}\\n")""")
      emit(s"""System.out.print("  -- Args:    ${argsList.mkString(" ")}\\n");""")
      while(examples.hasNext) {
        emit(s"""System.out.print("    -- Example: bash run.sh ${examples.next.mkString(" ")}\\n");""")  
      }
      emit(s"""System.exit(0);""")
    close("}")
  }

}

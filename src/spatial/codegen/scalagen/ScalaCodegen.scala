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
    def printableStms(stms: Seq[Sym[_]]): Seq[(Sym[_], Int)] = stms.map{x => (x, 1)} // Should scala be weighted also?
    def isLive(s: Sym[_], remaining: Seq[Sym[_]]): Boolean = !s.isMem && (b.result == s || remaining.exists(_.nestedInputs.contains(s)))
    def branchSfx(s: Sym[_], n: Option[String] = None): String = src""""${n.getOrElse(quote(s))}" -> $s"""
    def initChunkState(): Unit = {}

    val hierarchyDepth = (scala.math.log(printableStms(b.stms).map(_._2).sum) / scala.math.log(CODE_WINDOW)).toInt
    globalBlockID = javaStyleChunk[Sym[_]](
      printableStms(b.stms), 
      CODE_WINDOW, 
      hierarchyDepth, 
      globalBlockID, 
      isLive, 
      branchSfx, 
      remap, 
      () => initChunkState
    )(visit _ )
    
    if (withReturn) emit(src"${b.result}")
  }

  def emitPreMain(): Unit = { }
  def emitPostMain(): Unit = {
    emit("System.out.println(StatTracker)")
    emit("System.out.println(DRAMTracker)")
  }

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

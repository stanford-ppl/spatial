package spatial.codegen.scalagen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.metadata.CLIArgs
import spatial.lang._

trait ScalaCodegen extends Codegen with FileDependencies {
  override val lang: String = "scala"
  override val ext: String = "scala"

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  override def emitHeader(): Unit = {
    emit("import emul._")
    emit("import emul.implicits._")
    emit("")
    super.emitHeader()
  }

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
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
      open("def printHelp(): Unit = {")
        val argsList = CLIArgs.listNames
        val examples: Iterator[Seq[String]] = if (argsList.size > 0) IR.runtimeArgs.grouped(argsList.size) else Iterator(Seq(""))
        emit(s"""System.out.print("Help for app: ${config.name}\\n")""")
        emit(s"""System.out.print("  -- Args:    ${argsList.mkString(" ")}\\n");""")
        while(examples.hasNext) {
          emit(s"""System.out.print("    -- Example: bash run.sh ${examples.next.mkString(" ")}\\n");""")  
        }
        emit(s"""System.exit(0);""")
      close("}")
    close(src"}")
  }

}

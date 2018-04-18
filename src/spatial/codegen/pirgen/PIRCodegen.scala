package spatial.codegen.scalagen

import argon._
import argon.codegen.{Codegen, FileDependencies}

import spatial.lang._
import spatial.internal.spatialConfig

import scala.language.postfixOps
import scala.sys.process._

trait PIRCodegen extends Codegen with FileDependencies {
  override val lang: String = "scala"
  override val ext: String = "scala"

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  override def emitHeader(): Unit = {
    emit("import pir._")
    emit("import pir.node._")
    emit("import arch._")
    emit("import prism.enums._")
    emit("")
    open(src"""object ${config.name} extends PIRApp {""")
    //emit(s"""override val arch = SN_4x4""")
    open(src"""def main(implicit design:PIRDesign) = {""")
    emit(src"""import design.pirmeta._""")
    super.emitHeader()
  }

  override def emitFooter(): Unit = {
    super.emitHeader()
    emit(s"")
    close("}")
    close("}")
  }

  override protected def gen(b: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(b)
    if (withReturn) emit(src"${b.result}")
  }

  def emitPreMain(): Unit = { }
  def emitPostMain(): Unit = { }

  override protected def emitEntry(block: Block[_]): Unit = {
    emitPreMain()
    gen(block)
    emitPostMain()
  }

  def copyPIRSource = {
    if (sys.env.get("PIR_HOME").isDefined && sys.env("PIR_HOME") != "") {
      // what should be the cleaner way of doing this?
      val PIR_HOME = sys.env("PIR_HOME")
      //val dir = spatialConfig.pirsrc.getOrElse(s"$PIR_HOME/pir/apps/src")
      val dir = s"$PIR_HOME/pir/apps/src"
      var cmd = s"mkdir -p $dir"
      info(cmd)
      cmd.!

      cmd = s"cp ${config.genDir}/pir/main.scala $dir/${config.name}.scala"
      println(cmd)
      cmd.!
    }
    else {
      warn("Set PIR_HOME environment variable to automatically copy app")
    }
  }

  //def emit(lhs:Any, rhs:Any):Unit = {
    //emit(s"""val ${quote(lhs)} = $rhs$quoteCtrl.name("$lhs")""")
  //}
  //def emit(lhs:Any, rhs:Any, comment:Any):Unit = {
    //emit(s"""val ${quote(lhs)} = $rhs.name("$lhs")$quoteCtrl // $comment""")
  //}

}

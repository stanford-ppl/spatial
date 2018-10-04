package spatial.codegen.pirgen

import argon._
import argon.codegen.{Codegen, FileDependencies}
import spatial.codegen.naming.NamedCodegen
import spatial.metadata.CLIArgs
import spatial.metadata.memory._
import spatial.lang._
import spatial.util.spatialConfig

import scala.collection.mutable
import spatial.traversal.AccelTraversal

trait PIRCodegen extends Codegen with FileDependencies with NamedCodegen with AccelTraversal with PIRFormattedCodegen {
  override val lang: String = "pir"
  override val ext: String = "scala"
  final val CODE_WINDOW: Int = 75

  def and(ens: Set[Bit]): String = if (ens.isEmpty) "true" else ens.map(quote).mkString(" & ")

  private var globalBlockID: Int = 0

  // equivalent of quote
  //override def named(s: Sym[_], id: Int): String = {
    //super.named(s,id)
  //}

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

  override protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
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

  override protected def postprocess[R](b: Block[R]): Block[R] = {
    import scala.language.postfixOps
    import scala.sys.process._
    super.postprocess(b)
    //TODO: make pir a submodule
    //if (sys.env.get("PIR_HOME").isDefined && sys.env("PIR_HOME") != "") {
      //val PIR_HOME = sys.env("PIR_HOME")
      ////val dir = spatialConfig.pirsrc.getOrElse(s"$PIR_HOME/pir/apps/src")
      //val dir = s"$PIR_HOME/pir/apps/src"
      //var cmd = s"mkdir -p $dir"
      //info(cmd)
      //cmd.!

      //cmd = s"cp ${config.genDir}/pir/main.scala $dir/${config.name}.scala"
      //println(cmd)
      //cmd.!

      //cmd = s"rm $PIR_HOME/out/${config.name}/${config.name}.pir"
      //println(cmd)
      //cmd.!
    //} else {
      //warn("Set PIR_HOME environment variable to automatically copy app")
    //}

    b
  }

}

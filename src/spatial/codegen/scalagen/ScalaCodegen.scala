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
    val hierarchyDepth = (scala.math.log(b.stms.length) / scala.math.log(CODE_WINDOW)).toInt
    if (hierarchyDepth == 0) {
      visitBlock(b)
    }
    else if (hierarchyDepth == 1) {
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
    } else if (hierarchyDepth >= 2) { // TODO: Even more hierarchy
      println(s"crhunk of hierarchy 2! $b")
      globalBlockID += 1
      // TODO: More hierarchy? What if the block is > CODE_WINDOW * CODE_WINDOW * CODE_WINDOW size?
      val blockID: Int = globalBlockID
      var chunkID: Int = 0
      var chunk: Seq[Sym[_]] = Nil
      var remain: Seq[Sym[_]] = b.stms
      // TODO: Other ways to speed this up?
      def isLiveIn(s: Sym[_], remaining: Seq[Sym[_]]): Boolean = !s.isMem && (b.result == s || remaining.exists(_.nestedInputs.contains(s)))
      while (remain.nonEmpty) {
        var subChunkID: Int = 0
        var subChunk: Seq[Sym[_]] = Nil
        chunk = remain.take(CODE_WINDOW*CODE_WINDOW)
        remain = remain.drop(CODE_WINDOW*CODE_WINDOW)
        open(src"object Block${blockID}Chunker${chunkID} {")
        open(src"def gen(): Map[String, Any] = {")
        val live = chunk.filter(isLiveIn(_, remain))
        while (chunk.nonEmpty) {
          subChunk = chunk.take(CODE_WINDOW)
          chunk = chunk.drop(CODE_WINDOW)
          open(src"object Block${blockID}Chunker${chunkID}Sub${subChunkID} {")
            open(src"def gen(): Map[String, Any] = {")
            subChunk.foreach(visit)
            val subLive = subChunk.filter(isLiveIn(_, chunk ++ remain))
            emit("Map[String,Any](" + subLive.map{s => src""""$s" -> $s""" }.mkString(", ") + ")")
            scoped ++= subLive.map{s => s -> src"""block${blockID}chunk${chunkID}sub${subChunkID}("$s").asInstanceOf[${s.tp}]"""}
            close("}")
          close("}")
          emit(src"val block${blockID}chunk${chunkID}sub${subChunkID}: Map[String, Any] = Block${blockID}Chunker${chunkID}Sub${subChunkID}.gen()")
          subChunkID += 1
        }
        // Create map from unscopedName -> subscopedName
        val mapLHS = live.map{case x if (scoped.contains(x)) => val temp = scoped(x); scoped -= x; val n = quote(x); scoped += (x -> temp); n; case x => quote(x)}
        emit("Map[String,Any](" + mapLHS.zip(live).map{case (n,s) => src""""$n" -> $s""" }.mkString(", ") + ")")
        scoped ++= mapLHS.zip(live).map{case (n,s) => s -> src"""block${blockID}chunk$chunkID("$n").asInstanceOf[${s.tp}]"""}
        close("}")
        close("}")
        emit(src"val block${blockID}chunk${chunkID}: Map[String, Any] = Block${blockID}Chunker${chunkID}.gen()")
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

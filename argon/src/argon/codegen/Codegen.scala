package argon
package codegen

import java.io.PrintStream

import argon.passes.Traversal
import utils.io.files
import scala.collection._

trait Codegen extends Traversal {
  override val recurse: Recurse = Recurse.Never
  val lang: String
  def ext: String
  def out: String = s"${config.genDir}${files.sep}${lang}${files.sep}"
  def entryFile: String = s"Main.$ext"

  def clearGen(): Unit = {
    files.deleteExts(out, ext, recursive = true)
  }

  protected def emitEntry(block: Block[_]): Unit

  def emitHeader(): Unit = { }
  def emitFooter(): Unit = { }

  override protected def preprocess[R](b: Block[R]): Block[R] = {
    clearGen()
    super.preprocess(b)
  }

  override protected def postprocess[R](b: Block[R]): Block[R] = {
    super.postprocess(b)
  }

  protected def nameMap(x: String): String = x

  protected def remap(tp: Type[_]): String = tp.typeName

  protected def quoteConst(tp: Type[_], c: Any): String = {
    throw new Exception(s"$name failed to generate constant $c (${c.getClass}) of type $tp")
  }

  protected def named(s: Sym[_], id: Int): String = nameMap(s"x$id")

  protected def quote(s: Sym[_]): String = s.rhs match {
    case Def.TypeRef    => remap(s.tp)
    case Def.Const(c)   => quoteConst(s.tp, c)
    case Def.Param(_,c) => quoteConst(s.tp, c)
    case Def.Bound(id)  => s"b$id"
    case Def.Node(id,_) => named(s,id)
    case Def.Error(_,_) => throw new Exception(s"[$name] Error symbol in codegen")
  }

  protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Seq[_]     => p.map(quoteOrRemap).mkString(", ")  // By default, comma separate Seq
    case p: Array[_]   => p.map(quoteOrRemap).mkString(", ")
    case e: Ref[_,_]   => quote(e)
    case s: String     => s
    case c: Int        => c.toString
    case b: Boolean    => b.toString
    case l: Long       => l.toString + "L"
    case d: Double     => d.toString
    case l: BigDecimal => l.toString
    case l: BigInt     => l.toString
    case c: SrcCtx     => c.toString
    case None    => "None"
    case Some(x) => "Some(" + quoteOrRemap(x) + ")"
    case _ => throw new RuntimeException(s"[$name] Could not quote or remap $arg (${arg.getClass})")
  }

  implicit class CodegenHelper(sc: StringContext) {
    def src(args: Any*): String = sc.raw(args.map(quoteOrRemap): _*).stripMargin
  }

  override protected def process[R](block: Block[R]): Block[R] = {
    inGen(out, entryFile) {
      emitHeader()
      emitEntry(block)
      emitFooter()
    }
    block
  }

  protected def gen(block: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(block)
  }
  protected def ret(block: Block[_]): Unit = gen(block, withReturn = true)

  protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (config.enGen) throw new Exception(s"[$name] No codegen rule for $lhs, $rhs")
  }

  final override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = gen(lhs,rhs)

  protected val scoped: mutable.Map[Sym[_],ScopeInfo] = new mutable.HashMap[Sym[_],ScopeInfo]()

  case class ScopeInfo(val blockID: Int, val chunkID: Int, val subChunkID: Option[Int], val str: String, val tp: String){
    def assemble(sfx: String = ""): String = {
      val sub = if (subChunkID.isDefined) src"sub${subChunkID.get}" else ""
      src"block${blockID}chunk${chunkID}" + sub + src"""("${str + sfx}").asInstanceOf[$tp]"""
    }
  }
  case class StmWithWeight[X](val stm: X, val singleWeight: Int, val copies: Seq[String]){
    def weight: Int = (copies.size max 1) * singleWeight
  }

  final def javaStyleChunk[X](
    stmsAndWeights: Seq[StmWithWeight[X]],             // Seq of X to print and their associated "weight"
    code_window: Int,                          // Weighted X per window
    hierarchyDepth: Int,                       // Depth of chunker hierarchy
    globalBlockID: Int,                        // Global block ID for backend
    isLive: (X, Seq[X]) => Boolean,            // Check if X is escaping current chunk (X is in Seq AND not a "special" node)
    branchSfx: (X, Option[String]) => String,  // Create map entry from node name (unsuffixed) to node name (suffixed) (rhs requires .branch for chisel Switches)
    argString: (Type[_], Option[Sym[_]]) => String,                    // Name for X type (not exactly remap(tp) for chisel)
    initChunkState: () => Unit                    // Initialize state vars for chunk (i.e. ensig compression in chisel)
  )(visitRule: X => Unit): Int = {
    def fetchWindow(l: Seq[Int], limit: Int): Int = {
      @annotation.tailrec
      def take0(list: List[Int], accList: List[Int], accSum: Int) : Seq[Int] =
        list match {
          case h :: t if accSum + h < limit =>  
            take0(t, h :: accList, h + accSum)
          case _ => accList
        }
      take0(l.drop(1).toList, List(l.head), l.head).size
    }

    if (hierarchyDepth == 0) {
      stmsAndWeights.foreach{x => visitRule(x.stm)}
      globalBlockID
    }
    else if (hierarchyDepth == 1) {
      val blockID: Int = globalBlockID + 1
      var chunkID: Int = 0
      var chunk: Seq[StmWithWeight[X]] = Nil
      var remain: Seq[StmWithWeight[X]] = stmsAndWeights
      // TODO: Other ways to speed this up?
      while (remain.nonEmpty) {
        initChunkState()
        val num_stm = fetchWindow(remain.map(_.weight).toList, code_window)
        chunk = remain.take(num_stm)
        remain = remain.drop(num_stm)
        open(src"object Block${blockID}Chunker$chunkID { // ${chunk.size} nodes, ${chunk.map(_.weight).sum} weight")
          open(src"def gen(): Map[String, Any] = {")
          chunk.foreach{s => visitRule(s.stm) }
          val live: Seq[StmWithWeight[X]] = chunk.collect{case x if isLive(x.stm,remain.map(_.stm)) => x}
          emit("Map[String,Any](" + live.flatMap{case x if (x.copies.isEmpty) => Seq(branchSfx(x.stm,None)); case x => x.copies.map{sfx => src""""${x.stm}$sfx" -> ${x.stm}$sfx"""}}.mkString(", ") + ")")
          scoped ++= live.collect{case StmWithWeight(s: Sym[_], _,_) => s -> ScopeInfo(blockID, chunkID, None, quote(s), argString(s.tp, Some(s)))}
          close("}")
        close("}")
        emit(src"val block${blockID}chunk$chunkID: Map[String, Any] = Block${blockID}Chunker$chunkID.gen()")
        chunkID += 1
      }
      blockID
    }
    else {
      // TODO: More hierarchy? What if the block is > code_window * code_window * code_window size?
      val blockID: Int = globalBlockID
      var chunkID: Int = 0
      var chunk: Seq[StmWithWeight[X]] = Nil
      var remain: Seq[StmWithWeight[X]] = stmsAndWeights
    //   // TODO: Other ways to speed this up?
      while (remain.nonEmpty) {
        var subChunkID: Int = 0
        var subChunk: Seq[StmWithWeight[X]] = Nil
        val num_stm = fetchWindow(remain.map(_.weight).toList, code_window*code_window)
        chunk = remain.take(num_stm)
        remain = remain.drop(num_stm)
        open(src"object Block${blockID}Chunker${chunkID} { // ${chunk.size} nodes, ${chunk.map(_.weight).sum} weight")
        open(src"def gen(): Map[String, Any] = {")
        val live = chunk.collect{case x if isLive(x.stm,remain.map(_.stm)) => x}
        while (chunk.nonEmpty) {
          initChunkState()
          val subNum_stm = fetchWindow(chunk.map(_.weight).toList, code_window)
          subChunk = chunk.take(subNum_stm)
          chunk = chunk.drop(subNum_stm)
          open(src"object Block${blockID}Chunker${chunkID}Sub${subChunkID} { // ${subChunk.size} nodes, ${subChunk.map(_.weight).sum} weight")
            open(src"def gen(): Map[String, Any] = {")
            subChunk.foreach{s => visitRule(s.stm) }
            val subLive = subChunk.collect{case x if (isLive(x.stm, (chunk ++ remain).map(_.stm))) => x}
            emit("Map[String,Any](" + subLive.flatMap{case x if (x.copies.isEmpty) => Seq(branchSfx(x.stm,None)); case x => x.copies.map{sfx => src""""${x.stm}$sfx" -> ${x.stm}$sfx"""}}.mkString(", ") + ")")
            scoped ++= subLive.collect{case StmWithWeight(s: Sym[_], _,_) => s -> ScopeInfo(blockID, chunkID, Some(subChunkID), quote(s), argString(s.tp, Some(s)))}
            close("}")
          close("}")
          emit(src"val block${blockID}chunk${chunkID}sub${subChunkID}: Map[String, Any] = Block${blockID}Chunker${chunkID}Sub${subChunkID}.gen()")
          subChunkID += 1
        }
        // Create map from unscopedName -> subscopedName
        val mapLHS: Seq[String] = live.collect{case StmWithWeight(x: Sym[_], _,_) if (scoped.contains(x)) => val temp = scoped(x); scoped -= x;val n = quote(x); scoped += (x -> temp); n; case StmWithWeight(x: Sym[_], _,_) => quote(x)}
        emit("Map[String,Any](" + mapLHS.zip(live).flatMap{case (n,s) if (s.copies.isEmpty) => Seq(branchSfx(s.stm,Some(n))); case (n,s) => s.copies.map{sfx => src""""${n}$sfx" -> ${n}$sfx"""}}.mkString(", ") + ")")
        scoped ++= mapLHS.zip(live).collect{case (n,StmWithWeight(s: Sym[_],_,_)) => s -> ScopeInfo(blockID, chunkID, None, n, argString(s.tp, Some(s)))}
        close("}")
        close("}")
        emit(src"val block${blockID}chunk${chunkID}: Map[String, Any] = Block${blockID}Chunker${chunkID}.gen()")
        chunkID += 1
      }
      blockID + 1
    }
  }

  def kernel(sym: Sym[_]): PrintStream = getOrCreateStream(out, src"${sym}_kernel.$ext")
}

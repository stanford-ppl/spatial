package argon
package codegen

import java.io.PrintStream

import argon.passes.Traversal
import utils.io.files

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
    case o: Option[_] if !o.isDefined => "None"
    case o: Option[_] if o.isDefined => "Some(" + quoteOrRemap(o.get) + ")"
    case _ => throw new RuntimeException(s"[$name] Could not quote or remap $arg (${arg.getClass})")
  }

  implicit class CodegenHelper(sc: StringContext) {
    def src(args: Any*): String = sc.raw(args.map(quoteOrRemap): _*).stripMargin
  }

  override protected def process[R](block: Block[R]): Block[R] = {
    preprocess(block)

    inGen(out, entryFile) {
      emitHeader()
      emitEntry(block)
      emitFooter()
    }

    postprocess(block)
  }

  protected def gen(block: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(block)
  }
  protected def ret(block: Block[_]): Unit = gen(block, withReturn = true)

  protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    if (config.enGen) throw new Exception(s"[$name] No codegen rule for $lhs, $rhs")
  }

  final override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = gen(lhs,rhs)

  def kernel(sym: Sym[_]): PrintStream = getOrCreateStream(out, src"$sym.$ext")
}

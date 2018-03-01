package core
package passes

import forge.io.files

trait Codegen extends Traversal {
  override val recurse: Recurse = Recurse.Never
  val lang: String
  def ext: String
  def dir: String = s"${config.genDir}${files.sep}${lang}${files.sep}"
  def filename: String = s"Top.$ext"

  protected def nameMap(x: String): String = x

  protected def remap(tp: Type[_]): String = tp.typeName

  protected def quoteConst(tp: Type[_], c: Any): String = {
    throw new Exception(s"$name failed to generate constant $c of type $tp")
  }

  protected def named(s: Sym[_], id: Int): String = nameMap(s"x$id")

  protected def quote(s: Ref[_,_]): String = s.rhs match {
    case Def.TypeRef    => remap(s)
    case Def.Const(c)   => quoteConst(s.tp, c)
    case Def.Param(_,c) => quoteConst(s.tp, c)
    case Def.Bound(id)  => s"b$id"
    case Def.Node(id,_) => named(s,id)
    case Def.Error(_)   => throw new Exception(s"[$name] Error symbol in codegen")
  }

  protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Seq[_]   => p.map(quoteOrRemap).mkString(", ")  // By default, comma separate Seq
    case s: Set[_]   => s.map(quoteOrRemap).mkString(", ")  // TODO: Is this expected? Sets are unordered..
    case e: Ref[_,_] => quote(e)
    case s: String => s
    case c: Int => c.toString
    case b: Boolean => b.toString
    case l: Long => l.toString
    case l: BigDecimal => l.toString
    case l: BigInt => l.toString
    case _ => throw new RuntimeException(s"[$name] Could not quote or remap $arg (${arg.getClass})")
  }

  implicit class CodegenHelper(sc: StringContext) {
    def src(args: Any*): String = sc.raw(args.map(quoteOrRemap): _*).stripMargin
  }

  final override protected def process[R](block: Block[R]): Block[R] = {
    inGen(dir, filename) {
      gen(block)
    }
    block
  }

  protected def genResult(block: Block[_]): Unit = {
    throw new Exception(s"[$name] No rule for generating genResult defined")
  }

  protected def gen(block: Block[_], withReturn: Boolean = false): Unit = {
    visitBlock(block)
    if (withReturn) genResult(block)
  }
  protected def gen(lhs: Sym[_], rhs: Op[_]): Unit = {
    throw new Exception(s"[$name] No codegen rule for $lhs, $rhs")
  }

  final override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = gen(lhs,rhs)
}

package pcc
package traversal
package codegen

import util.files
import util.Tri._

trait Codegen extends Traversal {
  override val recurse: Recurse = Recurse.Never
  val lang: String
  val ext: String
  def out: String = s"${config.genDir}${files.sep}$lang${files.sep}"

  protected def nameMap(x: String): String = x

  protected def remap(tp: Sym[_]): String = tp.typeName

  protected def quoteConst(tp: Sym[_], c: Any): String = {
    throw new Exception(s"$name failed to generate constant $c of type $tp")
  }

  protected def named(s: Sym[_]): String = if (s.isBound) s"b${s.id}" else nameMap(s"x${s.id}")

  protected def quote(s: Sym[_]): String = s.rhs match {
    case Nix    => s"b${s.id}"
    case One(c) => quoteConst(s, c)
    case Two(_) => named(s)
  }

  protected def quoteOrRemap(arg: Any): String = arg match {
    case p: Seq[_] => p.map(quoteOrRemap).mkString(", ")  // By default, comma separate Seq
    case s: Set[_] => s.map(quoteOrRemap).mkString(", ")  // TODO: Is this expected? Sets are unordered..
    case e: Sym[_] if e.isType => quote(e)
    case s: String => s
    case c: Int => c.toString
    case b: Boolean => b.toString
    case l: Long => l.toString
    case l: BigDecimal => l.toString
    case l: BigInt => l.toString
    case _ => throw new RuntimeException(s"Could not quote or remap $arg (${arg.getClass})")
  }

  implicit class CodegenHelper(sc: StringContext) {
    def src(args: Any*): String = sc.raw(args.map(quoteOrRemap): _*).stripMargin
  }

}

package spatial.traversal

import argon._
import argon.passes.Traversal

import spatial.node._
import spatial.metadata.CLIArgs

case class CLINaming(IR: State) extends Traversal {

  private def traceName(lhs: Sym[_], idx: Int): Option[String] = lhs.name.orElse{
    if (idx > 6) None else {
      var out: Option[String] = None
      val deps = lhs.consumers.iterator
      while (deps.hasNext && out.isEmpty) {
        out = traceName(deps.next(), idx+1)
      }
      out
    }
  }

  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case ArrayApply(Op(InputArguments()), i) =>
      val argName = traceName(lhs,0)

      if (argName.isDefined) {
        val ii = i match {case Const(c) => c.toInt; case _ => -1}
        CLIArgs(ii) = argName.get
      }

    case _ => super.visit(lhs,rhs)
  }

}

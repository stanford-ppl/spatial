package forge.tags

import utils.tags.MacroUtils

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class flow extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro flow.impl
}

object flow {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import c.universe._
    import util._

    annottees.head match {
      case _:DefDef =>
      case _ => c.error(c.enclosingPosition, "@flow can only be used on defs")
    }
    def incorrectSignature(): Unit = {
      c.error(c.enclosingPosition, "@flow def must have signature 'def name(lhs: Sym[_], rhs: Op[_]): Unit")
    }

    val tree = api.impl(c)(annottees:_*) match {
      case d: DefDef =>
        val paramss = d.paramss
        if (paramss.length != 2) incorrectSignature()
        else if (paramss.head.length != 2) incorrectSignature()
        val arg0 = paramss.head.apply(0)
        val arg1 = paramss.head.apply(1)
        if (!arg0.tp.exists{t => isWildcardType(t, "Sym") } || !arg1.tp.exists{t => isWildcardType(t, "Op")}) {
          incorrectSignature()
        }

        val name = Literal(Constant(d.name.toString))

        val pf =
          q"""val ${d.name}: PartialFunction[(Sym[_],Op[_],forge.SrcCtx,argon.State),Unit] = {case (__sym,__op,__ctx,__state) =>
            val ${arg0.name} = __sym;
            val ${arg1.name} = __op;
            implicit val ctx = __ctx;
            implicit val state = __state;
            ${d.rhs}
          }
          """
        val add =
          q"""
             IR.flows.add($name,${d.name})
           """
        q"$pf; $add"

      case t =>
        __c.error(__c.enclosingPosition, "@flow can only be used on defs")
        t
    }
    //c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}

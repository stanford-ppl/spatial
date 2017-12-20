package forge

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * Requires implicit compiler state, but not necessarily source context
  */
final class stateful extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro stateful.impl
}

object stateful {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    val state = q"state: pcc.core.State"

    val tree = annottees.head match {
      case DefDef(mods,name,tparams,vparamss,tpt,rhs) =>
        val hasImplicits = vparamss.lastOption.exists(_.exists{x: ValDef => x.mods.hasFlag(Flag.IMPLICIT) })
        val params = if (hasImplicits) {
          val hasCtx = vparamss.lastOption.exists(_.exists{
            case ValDef(_,_,Ident(TypeName("State")),_) => true
            case ValDef(_,_,Select(Select(Ident(TermName("pcc")), TermName("core")), TypeName("State")), EmptyTree) => true
            case _ => false
          })
          if (!hasCtx) {
            vparamss.dropRight(1) :+ (vparamss.lastOption.getOrElse(Nil) ++ List(state))
          }
          else vparamss
        }
        else {
          vparamss :+ List(state)
        }
        q"$mods def $name[..$tparams](...${params.dropRight(1)})(implicit ..${params.last}): $tpt = $rhs"


      case ModuleDef(mods,name,Template(parents, selfType, bodyList)) =>
        val (fields, methods) = bodyList.partition { case _:DefDef => false case _ => true }
        val (constructors, defs) = methods.partition{ case DefDef(_,defName,_,_,_,_) => defName == termNames.CONSTRUCTOR }

        val defs2 = defs.map{method => stateful.impl(c)(method) }

        ModuleDef(mods, name, Template(parents, selfType, fields ++ constructors ++ defs2))

      case _ =>
        c.abort(c.enclosingPosition, "@stateful annotation can only be used on objects and defs")
    }
    tree
  }
}

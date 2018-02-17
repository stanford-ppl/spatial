package forge

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class ctx extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ctx.impl
}

private[forge] object ctx {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    val srcCtx = q"ctx: forge.SrcCtx"

    val tree = annottees.head match {
      case DefDef(mods,name,tparams,vparamss,tpt,rhs) =>
        val hasImplicits = vparamss.lastOption.exists(_.exists{x => x.mods.hasFlag(Flag.IMPLICIT) })
        val params = if (hasImplicits) {
          val hasCtx = vparamss.lastOption.exists(_.exists{
            case ValDef(_,_,Ident(TypeName(n)),_) => n == "SrcCtx"
            case ValDef(_,_,Select(Ident(TermName("forge")), TypeName("SrcCtx")), EmptyTree) => true
            case _ => false
          })
          if (!hasCtx) {
            vparamss.dropRight(1) :+ (vparamss.lastOption.getOrElse(Nil) ++ List(srcCtx))
          }
          else vparamss
        }
        else {
          vparamss :+ List(srcCtx)
        }
        q"$mods def $name[..$tparams](...${params.dropRight(1)})(implicit ..${params.last}): $tpt = $rhs"

      case _ =>
        c.abort(c.enclosingPosition, "API annotation can only be used on Def")
    }
    tree
  }
}
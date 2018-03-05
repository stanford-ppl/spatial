package forge.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox


class curriedUpdate extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro curriedUpdate.impl
}

object curriedUpdate {
  private val updateRenamed = "update$r"

  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    annottees.head match {
      case DefDef(mods, TermName(name), tparams, vparamss, tpt, rhs) =>
        if (name.toString != "update")
          c.abort(c.enclosingPosition, "curriedUpdate can only be applied to the update method")
        if (vparamss.size != 3)
          c.abort(c.enclosingPosition, "curriedUpdate must have three argument list")
        if (vparamss.head.isEmpty)
          c.abort(c.enclosingPosition, "The first argument list must not be empty")
        if (vparamss(1).size != 1)
          c.abort(c.enclosingPosition, "The second argument list must have only one element")
        if (vparamss(2).size != 2)
          c.abort(c.enclosingPosition, "The third argument list must have two elements")

        val imprt   = q"import scala.language.experimental.macros"
        val updateR = DefDef(mods, TermName(updateRenamed), tparams, vparamss, tpt, rhs)
        val updateC = if (mods.hasFlag(Flag.OVERRIDE)) {
          q"override def update(values: Any*): Unit = macro forge.tags.curriedUpdate.updateMacroDispatcher"
        }
        else {
          q"def update(values: Any*): Unit = macro forge.tags.curriedUpdate.updateMacroDispatcher"
        }

        val result = q"$imprt ; $updateR ; $updateC"

        //c.info(c.enclosingPosition, showCode(result), true)
        result

      case _ =>
        c.abort(c.enclosingPosition, "curriedUpdate can only be applied on defs")
    }
  }

  def updateMacroDispatcher(c: blackbox.Context)(values: c.Tree*): c.Tree = {
    import c.universe._

    val inds = values.take(values.size - 3)
    val value = values(values.size - 3)
    val ctx = values(values.size - 2)
    val state = values(values.size - 1)
    val self = c.prefix.tree
    q"$self.${TermName(updateRenamed)}(..$inds)($value)($ctx, $state)"
  }
}

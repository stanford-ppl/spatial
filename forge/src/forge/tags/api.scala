package forge.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * Annotates an entry point from the user's program to the compiler
  * Optionally adds implicit SourceContext parameter if one does not already exist (since all API methods need this)
  */
final class api extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro api.impl
}

private[forge] object api {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val utils = MacroUtils[c.type](c)
    import utils._
    import c.universe._

    annottees.head match {
      case df: DefDef =>
        val withCtx   = ctx.impl(c)(annottees:_*)
        val withState = stateful.impl(c)(withCtx)
        withState
      case _ => invalidAnnotationUse("api", "defs")
    }
  }
}

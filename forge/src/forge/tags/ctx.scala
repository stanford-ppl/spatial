package forge.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class ctx extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ctx.impl
}

private[forge] object ctx {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import util._
    import c.universe._

    val tree = annottees.head match {
      case df: DefDef => df.injectImplicit("ctx", tq"forge.SrcCtx", tq"SrcCtx")
      case _ => invalidAnnotationUse("ctx", "defs")
    }
    tree
  }
}
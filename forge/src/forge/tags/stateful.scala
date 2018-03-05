package forge.tags

import utils.tags.MacroUtils

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
  * Requires implicit compiler state, but not necessarily source context
  */
final class stateful extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro stateful.impl
}

private[forge] object stateful {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import util._
    import c.universe._

    def injectState(df: DefDef) = df.injectImplicit("state", tq"core.State", tq"State")

    val tree = annottees.head match {
      case df: DefDef    => injectState(df)
      case mf: ModuleDef => mf.mapMethods(injectState)
      case _ => invalidAnnotationUse("stateful", "objects", "defs")
    }
    tree
  }
}

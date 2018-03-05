package forge.tags

import utils.tags.MacroUtils

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class data extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro data.impl
}

object data {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val utils = new MacroUtils(c)
    import utils._
    import c.universe._
    annottees.head match {
      case _:ModuleDef => stateful.impl(c)(annottees:_*)
      case _ => invalidAnnotationUse("data", "objects")
    }
  }
}

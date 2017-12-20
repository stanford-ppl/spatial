package forge

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class data extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro data.impl
}

object data {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._
    annottees.head match {
      case ModuleDef(_,_,_) => stateful.impl(c)(annottees:_*)
      case _ => c.abort(c.enclosingPosition, "@data annotation can only be used on objects")
    }
  }
}

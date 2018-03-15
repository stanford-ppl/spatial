package utils.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class instrument extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro instrument.impl
}

private[utils] object instrument {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import util._
    import c.universe._

    def instrument(df: DefDef): DefDef = {
      if (df.body != EmptyTree) df.modifyBody{body => q"instrument(${df.nameLiteral}){ $body }"}
      else df
    }

    val tree = annottees.head match {
      case cls: ClassDef  => cls.mapMethods(m => instrument(m)).mixIn(tq"utils.Instrumented")
      case obj: ModuleDef => obj.mapMethods(m => instrument(m)).mixIn(tq"utils.Instrumented")
      case _ => invalidAnnotationUse("@instrument", "objects", "defs")
    }
    //c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}

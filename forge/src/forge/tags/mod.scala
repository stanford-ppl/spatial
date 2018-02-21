package forge.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * Generates a couple of boiler plate methods for module types:
  *
  */
final class mod extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro mod.impl
}
object mod {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import c.universe._
    import util._

    val tree = annottees.head match {
      case cls: ClassDef =>
        val names = cls.constructorArgs.head.map(_.name)

        cls.asCaseClass.injectMethod(q"final override def names = Seq(..$names)")

      case t =>
        c.error(c.enclosingPosition, "@mod annotation can only be used on classes.")
        t
    }

    c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}

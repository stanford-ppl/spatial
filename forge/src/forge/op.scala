package forge

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros


final class op extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro op.impl
}

object op {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = utils[c.type](c)
    import c.universe._
    import util._

    val tree = annottees.head match {
      case cls: ClassDef =>
        cls.asCaseClass.injectMethod {(_,tp) =>
          val names = cls.constructorArgs.head.map(_.name)
          val mirrored = names.map{name => q"f($name)" }
          val name = cls.name
          q"override def mirror(f:Tx) = new $name(..$mirrored)"
        }
      case t =>
        c.error(c.enclosingPosition, "@mod can only be used on class definitions")
        t
    }
    //c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}
package forge.tags

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

final class op extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro op.impl
}

object op {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import c.universe._
    import util._

    val tree = annottees.head match {
      case cls: ClassDef =>
        val name = cls.name
        val names = cls.constructorArgs.head.map(_.name)
        val targs = cls.typeArgs
        val fnames = names.map{name => q"f($name)" }
        val updates = names.zip(fnames).map{case (name,fname) => q"$name = $fname" }

        cls.asCaseClass.withVarParams
           .injectMethod(q"override def mirror(f:Tx) = new $name[..$targs](..$fnames)")
           .injectMethod(q"override def update(f:Tx) = { ..$updates }")

      case t =>
        c.error(c.enclosingPosition, "@op can only be used on class definitions")
        t
    }
    //c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}
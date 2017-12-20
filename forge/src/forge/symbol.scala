package forge

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

/**
  * Generates a couple of boiler plate methods for symbol types:
  *
  */
/*final class symbol extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro symbol.impl
}

object symbol {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = utils[c.type](c)
    import c.universe._
    import util._

    val tree = annottees.head match {
      case cls: ClassDef =>
        val args = cls.constructorArgs.filter{group => !group.exists{v => v.mods.hasFlag(Flag.IMPLICIT)}}
        if (args.length != 1 || args.head.length != 1) {
          c.abort(c.enclosingPosition, s"@sym classes must have exactly one constructor argument of type Int.\n" +
            s"${cls.name} had ${args.length} curried groups with a total of ${args.map(_.length).sum} arguments.")
        }
        if (args.head.head.tp.get match {case Ident(TermName("Int")) => true case _ => false }) {
          c.error(c.enclosingPosition, s"Int: ${showRaw(makeType("Int"))}")
          c.abort(c.enclosingPosition, s"@sym classes must have exactly one constructor argument of type Int.\n" +
            s"${cls.name}'s argument is of type ${showRaw(args.head.head.tp.get)}")
        }

        cls.asCaseClass.injectMethod{(name,tp) =>
            q"final override def stagedClass = classOf[$tp]"
           }
           .injectMethod{(name,tp) =>
             q"final override def fresh(id: Int) = ${cls.callConstructor(q"id")}"
           }

      case t =>
        c.error(c.enclosingPosition, "@sym annotation can only be used on classes.")
        t
    }

    c.info(c.enclosingPosition, showCode(tree), force = true)
    tree
  }
}*/
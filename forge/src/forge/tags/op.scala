package forge.tags

import utils.tags.MacroUtils

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

    val (cls,obj) = annottees.toList match {
      case List(cd: ClassDef, md: ModuleDef) => (cd,md)
      case List(cd: ClassDef) => (cd, q"object ${cd.nameTerm}".asObject)
      case _ => invalidAnnotationUse("ref", "classes")
    }

    val name = cls.name
    val names = cls.constructorArgs.head.map(_.name)
    val types = cls.constructorArgs.head.map(_.tp.get)
    val targs = cls.typeArgs
    val fnames = names.map{name => q"$$f($name)" }
    val updates = names.zip(fnames).map{case (name,fname) => q"$name = $fname" }

    val cls2 = cls.asCaseClass.withVarParams
      .injectMethod(q"override def mirror($$f:Tx) = new $name[..$targs](..$fnames)".asDef)
      .injectMethod(q"override def update($$f:Tx) = { ..$updates }".asDef)

    val obj2 = obj

    // TODO[5]: Not clear how this would be typed, or if its even an intuitive extractor
    /*val obj2 = obj.injectMethod(
      q"""def unapply[..$targs](s: Sym[_]): Option[(..$types)] = s match {
            case Op(${cls.nameTerm}(..$names)) => Some((..$names))
            case _ => None
          }""".asDef)*/

    c.info(c.enclosingPosition, showCode(cls2), force = true)

    q"..${List(cls2,obj2)}"
  }
}
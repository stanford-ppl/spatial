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
    val targs = cls.typeArgs
    val fnames = names.map{name => q"$$f($name)" }
    val updates = names.zip(fnames).map{case (name,fname) => q"$name = $fname" }

    val cls2 = cls.asCaseClass.withVarParams
      .injectMethod(q"override def mirror($$f:Tx) = new $name[..$targs](..$fnames)".asDef)
      .injectMethod(q"override def update($$f:Tx) = { ..$updates }".asDef)

    q"..${List(cls2,obj)}"
  }
}
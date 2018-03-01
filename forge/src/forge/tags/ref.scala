package forge.tags

import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox

/** Annotation class for @ref macro annotation. */
final class ref extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro ref.impl
}

/** Companion object implementing @ref macro annotation. */
private object ref {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new MacroUtils[c.type](c)
    import util._
    import c.universe._

    val (cls,obj) = annottees.toList match {
      case List(cd: ClassDef, md: ModuleDef) => (cd,md)
      case List(cd: ClassDef) => (cd, q"object ${cd.nameTerm}".asObject)
      case _ => invalidAnnotationUse("ref", "classes")
    }
    val (vargs,iargs) = cls.constructorArgs match {
      case List(v,i) if i.isImplicit => (v,i)
      case List(v) => (v, Nil)
      case _ => c.abort(c.enclosingPosition, "Ref classes can have at most one explicit and one implicit parameter list")
    }
    // TODO: Should check that @ref class A mixes in Ref[?,A]

    val name = cls.name
    val tparams = cls.tparams
    val targs = cls.typeArgs
    val vnames = vargs.map(_.name)
    val inames = iargs.map(_.name)
    val cnames = vnames ++ inames
    val fullName = targsType(name, targs)

    val cls2 = cls.injectMethod(q"private def cargs: Seq[Any] = Seq(..$cnames)".asDef)
                  .injectMethod(q"override protected def fresh = new $name[..$targs](..$vnames)".asDef)
                  .injectMethod(q"override def typePrefix = ${cls.nameLiteral}".asDef)
                  .injectMethod(q"override def typeArgs = cargs.collect{case t: Type[_] => t}".asDef)

    val obj2 = (vargs, iargs) match {
      case (Nil,Nil) => obj.injectField(q"implicit val tp: $fullName = (new $name[..$targs]).asType".asVal)
      case (Nil, _)  => obj.injectMethod(q"implicit def tp[..$tparams](..$iargs): $fullName = (new $name[..$targs]()(..$inames)).asType".asDef)
      case (_, Nil)  => obj.injectMethod(q"def tp[..$tparams](..$vargs): $fullName = (new $name[..$targs](..$vnames)).asType".asDef)
      case _ =>         obj.injectMethod(q"def tp[..$tparams](..$vargs)(..$iargs): $fullName = (new $name[..$targs](..$vnames)(..$inames)).asType".asDef)
    }

    //c.info(c.enclosingPosition, showCode(cls2), force = true)
    //c.info(c.enclosingPosition, showCode(obj2), force = true)

    q"..${List(cls2,obj2)}"
  }
}

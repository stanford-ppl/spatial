package spatial.tags

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

import utils.tags.MacroUtils

/** Annotation class for @struct macro annotation. */
final class struct extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro StagedStructsMacro.impl
}

/**
  * Towards modularity:
  * Defining and composing macros in different DSLs
  *
  * Want possible type classes for struct to be generated here, based on the type classes defined/available in the DSL
  * This should eventually be generated from Forge (?), for now just outlining how it might work.
  */
abstract class TypeclassMacro {
  def generateLookup(c: blackbox.Context)(name: c.TypeName): Option[c.Tree]
  def generateImplementation(c: blackbox.Context)(className: c.Tree): List[c.Tree]
}


object StagedStructsMacro {
  val typeclasses: List[TypeclassMacro] = Nil //List(BitsTypeclassMacro, ArithsTypeclassMacro)

  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val utils = new MacroUtils[c.type](c)
    import utils._
    import c.universe._

    val (cls,obj) = annottees.toList match {
      case List(cd: ClassDef, md: ModuleDef) => (cd,md)
      case List(cd: ClassDef) => (cd, q"object ${cd.nameTerm}".asObject)
      case _ => invalidAnnotationUse("struct", "classes")
    }

    val fields = cls.constructorArgs.head
    val methods = cls.methods

    if (fields.isEmpty) abort("Classes need at least one field in order to be transformed into an @struct.")
    if (methods.nonEmpty) abort("@struct classes with methods are not yet supported.")
    if (cls.tparams.nonEmpty) abort("@struct classes with type parameters are not yet supported")
    if (cls.fields.exists(_.isVar)) abort("@struct classes with var fields are not yet supported")

    val fieldTypes  = fields.map{field => q"${field.nameLiteral} -> argon.Type[${field.tpTree}]" }
    val fieldNames  = fields.map{field => q"${field.nameLiteral} -> ${field.name}"}
    val fieldOpts   = fields.map{field => field.withRHS(q"null") }
    val fieldOrElse = fields.map{field => q"Option(${field.name}).getOrElse{this.${field.name}}" }

    var cls2 = q"class ${cls.name}[..${cls.tparams}]()".asClass
    var obj2 = obj
    fields.foreach{field =>
      cls2 = cls2.injectMethod(
            q"""def ${field.name}(implicit ctx: forge.SrcCtx, state: argon.State): ${field.tpTree} = {
                  field[${field.tpTree}](${field.nameLiteral})(argon.Type[${field.tpTree}],ctx,state)
                }""".asDef)
    }
    cls2 = {
      cls2.injectField(q"val fields = Seq(..$fieldTypes)".asVal)
          .mixIn(q"spatial.lang.Struct[${cls.fullName}]")
          .mixIn(q"argon.Ref[Any,${cls.fullName}]")
          .injectMethod(
            q"""def copy(..$fieldOpts)(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                  className.toTermName.apply(..$fieldOrElse)(ctx, state)
                }""".asDef)
    }

    obj2 = {
      obj2.injectMethod(
        q"""def apply[..${cls.tparams}](..$fields)(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
              spatial.lang.Struct[${cls.fullName}]( ..$fieldNames )
            }
         """.asDef)
    }

    forge.tags.ref.impl(c)(q"..${List(cls2,obj2)}")
  }
}

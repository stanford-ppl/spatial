package argon.tags

import utils.tags.MacroUtils

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

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
abstract class TypeclassMacro[Ctx <: blackbox.Context](val c: Ctx) {
  import c.universe._
  def implement(cls: ClassDef, obj: ModuleDef, fields: Seq[ValDef]): (ClassDef,ModuleDef)
}


object StagedStructsMacro {

  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val typeclasses: Seq[TypeclassMacro[c.type]] = Seq(
      new Bits[c.type](c),
      new Arith[c.type](c)
    )

    val utils = new MacroUtils[c.type](c)
    import c.universe._
    import utils._

    val (cls,obj) = annottees.toList match {
      case List(cd: ClassDef, md: ModuleDef) => (cd,md)
      case List(cd: ClassDef) => (cd, q"object ${cd.nameTerm}".asObject)
      case _ => invalidAnnotationUse("struct", "classes")
    }

    val fields = cls.constructorArgs.head
    val methods = cls.nonConstructorMethods.filterNot{_.mods.hasFlag(Flag.CASEACCESSOR) }
    val parents: Seq[String] = cls.parents.collect{case Ident(TypeName(name)) => name }

    // TODO[5]: What to do if class has parents? Error?

    if (fields.isEmpty) abort("Classes need at least one field in order to be transformed into a @struct.")
    if (methods.nonEmpty) {
      error(s"@struct class had ${methods.length} disallowed methods:\n" + methods.map{method =>
        "  " + showCode(method.modifyBody(_ => EmptyTree))
      }.mkString("\n"))
      abort("@struct classes with methods are not yet supported")
    }
    if (cls.tparams.nonEmpty) abort("@struct classes with type parameters are not yet supported")
    if (cls.fields.exists(_.isVar)) abort("@struct classes with var fields are not yet supported")

    val fieldTypes  = fields.map{field => q"${field.nameLiteral} -> argon.Type[${field.tpTree}]" }
    val fieldNames  = fields.map{field => q"${field.nameLiteral} -> ${field.name}"}
    val fieldOpts   = fields.map{field => field.withRHS(q"null") }
    val fieldOrElse = fields.map{field => q"Option(${field.name}).getOrElse{this.${field.name}(ctx,state)}" }
    val fieldCases = fields.zip(fieldOrElse).map{case (f,foe) =>
      val pat = pq"${f.name}"
      cq"$pat => $foe;"
    }

    var cls2 = q"class ${cls.name}[..${cls.tparams}]() extends spatial.lang.Struct[${cls.fullName}]".asClass
    var obj2 = obj
    fields.foreach{field =>
      cls2 = cls2.injectMethod(
            q"""def ${field.name}(implicit ctx: forge.SrcCtx, state: argon.State): ${field.tpTree} = {
                  field[${field.tpTree}](${field.nameLiteral})(argon.Type[${field.tpTree}],ctx,state)
                }""".asDef)
    }
//    cls2 = cls2.injectMethod(
//          q"""def getField(field: String)(implicit ctx: forge.SrcCtx, state: argon.State): Any = {
//             field match {
//               case ..$fieldCases
//             }}""".asDef)
    cls2 = {
      cls2.injectField(q"lazy val fields = Seq(..$fieldTypes)".asVal)
          .mixIn(tq"argon.Ref[Any,${cls.fullName}]")
          .injectMethod(
            q"""def copy(..$fieldOpts)(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                  ${obj2.name}.apply(..$fieldOrElse)(ctx, state)
                }""".asDef)
          .injectField(q"""override val box = implicitly[${cls.fullName} <:< (
                             spatial.lang.Struct[${cls.fullName}]
                        with argon.lang.types.Bits[${cls.fullName}]
                        with argon.lang.types.Arith[${cls.fullName}]) ]""".asVal)
    }

    obj2 = {
      obj2.injectMethod(
        q"""def apply[..${cls.tparams}](..$fields)(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
              spatial.lang.Struct[${cls.fullName}]( ..$fieldNames )(spatial.lang.Struct.tp[${cls.fullName}], ctx, state)
            }
         """.asDef)
    }

    val (cls3,obj3) = typeclasses.foldRight((cls2,obj2)){case (tc, (c,o)) =>
      tc.implement(c, o, fields)
    }

    val (cls4, obj4) = forge.tags.ref.implement(c)(cls3, obj3)
    val out = q"$cls4; $obj4"

    //info(showRaw(out))
    //info(showCode(out))

    out
  }
}

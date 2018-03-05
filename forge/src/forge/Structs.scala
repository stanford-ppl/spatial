package forge

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/** Annotation class for @struct macro annotation. */
// TODO[2]: Update for new Exp/Type
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
    import c.universe._

    if (annottees.length > 1)
      c.abort(c.enclosingPosition, "Only classes can be transformed using the @struct annotation")

    val tree = annottees.head
    tree match {
      // look for class definition
      case ClassDef(mods, className: TypeName, tparams, impl@Template(parents, selfType, bodyList)) =>
        val (fields, methods) = bodyList.partition { case _:ValDef => true case _ => false }
        if (fields.isEmpty)
          c.abort(c.enclosingPosition, "Classes need at least one field in order to be transformed into structs")
        if (methods.size > 1)
          c.abort(c.enclosingPosition, "Classes with a body (e.g. methods) cannot be transformed into structs")
        if (tparams.size > 0)
          c.abort(c.enclosingPosition, "Classes with type parameters cannot be transformed into structs")

        assert(methods.head match { case _: DefDef => true }) // the constructor

        val constructorArgs = methods.collect{
          case DefDef(ms,name,tparams,argss,_,_) if name == termNames.CONSTRUCTOR => argss
        }.head

        /**
          * Staged class definition
          */
        val fieldList = fields map {
          case ValDef(mods, termName, typeIdent, rhs) if mods.hasFlag(Flag.MUTABLE) =>
            //q"var $termName: $typeIdent"
            c.abort(c.enclosingPosition, "virtualization of variable fields is currently unsupported")
          case ValDef(mods, termName, typeIdent, rhs) =>
            q"""def $termName(implicit ctx: org.virtualized.SourceContext, state: argon.core.State): $typeIdent = field[$typeIdent](${Literal(Constant(termName.toString))})(implicitly[Type[$typeIdent]],ctx,state)"""
        }

        /**
          * Typeclass evidences and lookup traits
          */
        val evidences = typeclasses.flatMap{_.generateImplementation(c)(tree) }

        /**
          * Staged type
          */
        val childList = fields.map{
          case ValDef(_, termName, typeIdent, rhs) =>
            q""" ${Literal(Constant(termName.toString))} -> typ[$typeIdent]"""
        }

        val lookups = typeclasses.flatMap(_.generateLookup(c)(className) )
        val parent = {
          if (lookups.length > 1) CompoundTypeTree(Template(lookups, noSelfType, Nil))
          else lookups.head
        }

        val stg =
          q"""
            object ${TermName(className.toString + "Type")} extends argon.nodes.StructType[$className] with $parent {
              override def wrapped(x: Exp[$className]) = ${className.toTermName}(x)
              override def typeArguments = Nil
              override def stagedClass = classOf[$className]
              override def fields = Seq(..$childList)
            }
           """
        /**
          * Staged Evidence
          */
        val ev =
          q"""
            implicit def ${TermName(className.toString + "TypeEvidence")}: argon.nodes.StructType[$className] = ${TermName(className.toString + "Type")}
          """

        /**
          * Constructor
          */
        val argss = constructorArgs.map{ args => args.map{
          case ValDef(_, termName, typeIdent, rhs) =>
            val mods = if (rhs == EmptyTree) Modifiers(Flag.PARAM) else Modifiers(Flag.PARAM | Flag.DEFAULTPARAM)
            val rhsHack = if (rhs == EmptyTree) EmptyTree else q"implicitly[Type[$typeIdent]].fakeT"
            ValDef(mods, termName, typeIdent, rhsHack)
        }}
        val body = constructorArgs.flatten.map{
          case ValDef(_, termName, typeIdent, EmptyTree) =>
            q"${Literal(Constant(termName.toString))} -> $termName.s"
          case ValDef(_, termName, typeIdent, rhs) =>
            q"${Literal(Constant(termName.toString))} -> $termName.getOrElseCreate{$rhs}.s"
        }


        val mdef = q"def apply(...$argss)(implicit ctx: org.virtualized.SourceContext, state: argon.core.State): $className = struct[$className]( ..$body )(implicitly[argon.nodes.StructType[$className]], ctx, state)"

        val companionObject = q"""
          object ${className.toTermName} {
            $stg ; $mdef; $ev ; ..$evidences
          }
        """

        val copyMethod = {
          val optionalArgss = constructorArgs.map{args => args.map{
            case ValDef(_, termName, typeIdent, rhs) =>
              ValDef(Modifiers(Flag.PARAM | Flag.DEFAULTPARAM), termName, typeIdent, q"implicitly[Type[$typeIdent]].fakeT") //q"this.$termName")
          }}
          val actualArgss = constructorArgs.map{args => args.map{
            case ValDef(_, termName, _, _) => q"$termName.getOrElseCreate{ this.$termName(ctx, state) }"
          }}

          q"""
             def copy(...$optionalArgss)(implicit ctx: org.virtualized.SourceContext, state: argon.core.State): $className = {
               ${className.toTermName}.apply(...$actualArgss)(ctx, state)
             }
           """
        }

        val cls =
          q"""
            case class $className(s: Exp[$className]) extends Struct[$className] {
              ..$fieldList ; $copyMethod
            }
           """

        // Implicit object must come before class definition
        val cc = q"$cls ; $companionObject "

        // Debugging
        // c.info(tree.pos, showCode(cc), force = true)
        // c.info(tree.pos, showRaw(mdef), force = true)
        cc

      case _ =>
        c.error(c.enclosingPosition, "Only classes can be transformed using the @struct annotation")
        annottees.head
    }
  }
}

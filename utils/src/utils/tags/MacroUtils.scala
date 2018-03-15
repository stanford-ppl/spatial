package utils.tags

import utils.conj
import utils.implicits.collections._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class MacroUtils[Ctx <: blackbox.Context](val __c: Ctx) {
  import __c.universe._

  def methodCall(recOpt: Option[Tree], methName: String, args: List[List[Tree]], targs: List[Tree] = Nil): Tree = {
    val calleeName = TermName(methName)
    val callee = recOpt match {
      case Some(rec) => Select(rec, calleeName)
      case None      => Ident(calleeName)
    }
    val calleeAndTargs: Tree = targsCall(callee, targs)
    args.foldLeft(calleeAndTargs) { Apply(_, _) }
  }

  def targsCall(select: Tree, targs: List[Tree]): Tree = {
    if (targs.nonEmpty) TypeApply(select, targs) else select
  }

  def targsType(select: TypeName, targs: List[Tree]): Tree = tq"$select[..$targs]"

  // Hack for scala bug #10589 where <caseaccessor> gets added to (private) implicit fields
  def fieldsFix(fields: List[ValDef]): List[ValDef] = fields.map{
    case ValDef(mods,name,tp,rhs) if mods.hasFlag(Flag.CASEACCESSOR) &&
                                     mods.hasFlag(Flag.PARAMACCESSOR) &&
                                     mods.hasFlag(Flag.IMPLICIT) &&
                                     mods.hasFlag(Flag.LOCAL) &&
                                     mods.hasFlag(Flag.PROTECTED) =>

      val flags = Modifiers(Flag.IMPLICIT | Flag.PARAMACCESSOR | Flag.LOCAL | Flag.PROTECTED)
      ValDef(flags, name, tp, rhs)

    case ValDef(mods,name,tp,rhs) if mods.hasFlag(Flag.CASEACCESSOR) &&
                                     mods.hasFlag(Flag.PARAMACCESSOR) &&
                                     mods.hasFlag(Flag.PRIVATE) &&
                                     mods.hasFlag(Flag.IMPLICIT) &&
                                     mods.hasFlag(Flag.LOCAL) =>
      val flags = Modifiers(Flag.PRIVATE | Flag.IMPLICIT | Flag.PARAMACCESSOR | Flag.LOCAL)
      ValDef(flags, name, tp, rhs)

    //case f if f.mods.hasFlag(Flag.IMPLICIT) =>
    //  __c.info(__c.enclosingPosition, showRaw(f), force = true)
    //  __c.info(__c.enclosingPosition, showCode(f), force = true)
    //  f

    case v => v
  }

  def isWildcardType(tp: Tree, str: String): Boolean = tp match {
    case ExistentialTypeTree(AppliedTypeTree(Ident(TypeName(`str`)), List(Ident(TypeName(arg)))), _) => arg.startsWith("_$")
    case _ => false
  }

  implicit class ModiferOps(x: Modifiers) {
    def isImplicit: Boolean = x.hasFlag(Flag.IMPLICIT)
    def withMutable: Modifiers = Modifiers(x.flags | Flag.MUTABLE)
    def withCase: Modifiers = Modifiers(x.flags | Flag.CASE)
  }
  object Mods {
    def implicitParam: Modifiers = Modifiers(Flag.IMPLICIT | Flag.PARAM)
  }

  trait NamedOps {
    def nameTerm: TermName
    def nameStr: String
    def nameLiteral: Literal = Literal(Constant(nameStr))
  }

  implicit class SigOps(vs: List[ValDef]) {
    def isImplicit: Boolean = vs.exists(_.isImplicit)
  }

  implicit class ValDefOps(v: ValDef) extends NamedOps {
    val ValDef(mods,nameTerm@TermName(nameStr),tpTree,rhs) = v

    def tp: Option[Tree] = if (tpTree == EmptyTree) None else Some(tpTree)

    def asVar: ValDef = ValDef(mods.withMutable,nameTerm,tpTree,rhs)

    def hasType(types: Seq[Tree]): Boolean = types.exists{tp => tp equalsStructure tpTree }

    def isImplicit: Boolean = mods.hasFlag(Flag.IMPLICIT)
  }
  object Param {
    def impl(name: String, tp: Tree, rhs: Tree = EmptyTree): ValDef = {
      ValDef(Mods.implicitParam, TermName(name), tp, rhs)
    }
  }

  implicit class DefDefOps(df: DefDef) extends NamedOps {
    val DefDef(mods:    Modifiers,
               nameTerm@TermName(nameStr: String),
               tparams, // List[TypeDef],
               paramss, // List[List[ValDef]],
               retTp,   // Tree
               body     // Tree
              ) = df

    def hasImplicits: Boolean = paramss.lastOption.iterator.flatten.exists(_.mods.isImplicit)
    def implicits: Option[List[ValDef]] = if (hasImplicits) Some(paramss.last) else None

    def injectImplicit(name: String, types: Tree*): DefDef = {
      lazy val param: List[ValDef] = List(Param.impl(name,types.head,EmptyTree))

      val paramss2: List[List[ValDef]] = df.implicits match {
        case Some(imps) if !imps.exists(_.hasType(types)) => paramss.dropRight(1) :+ (imps ++ param)
        case Some(_) => paramss
        case None    => paramss :+ param
      }
      DefDef(mods,nameTerm,tparams,paramss2,retTp,body)
    }

    def isConstructor: Boolean = nameTerm == termNames.CONSTRUCTOR

    def modifyBody(func: Tree => Tree): DefDef = {
      val body2 = func(body)
      DefDef(mods,nameTerm,tparams,paramss,retTp,body2)
    }
  }

  trait Templated[A] {
    def cls: A
    def template: Template
    lazy val Template(
      parents,   // List[Tree]
      selfType,  // ValDef
      __body     // List[Tree] (ValDefs and DefDefs)
    ) = template
    lazy val fields: List[ValDef] = fieldsFix(__body.collect{case x: ValDef => x })
    lazy val methods: List[DefDef] = __body.collect{case x: DefDef => x }
    lazy val other: List[Tree] = __body.filter{case _:ValDef|_:DefDef => false; case _ => true }
    lazy val body: List[Tree] = fields ++ methods ++ other

    def fieldsAndMethods: (List[ValDef],List[DefDef]) = (fields, methods)
    def fieldAndMethodNames: List[String] = fields.map(_.nameStr) ++ methods.map(_.nameStr)

    def constructors: List[DefDef] = methods.filter(_.name == termNames.CONSTRUCTOR)
    def nonConstructorMethods: List[DefDef] = methods.filterNot(_.name == termNames.CONSTRUCTOR)

    def constructor: Option[DefDef] = constructors.headOption
    def constructorArgs: List[List[ValDef]] = constructor.map{d => d.paramss }.getOrElse(Nil)

    def injectStm(tree: Tree): A = copyWithBody(tree +: body)

    def injectStmLast(tree: Tree): A = copyWithBody(body :+ tree)

    def injectField(field: ValDef): A = {
      if (!fieldAndMethodNames.contains(field.nameStr)) copyWithBody(body :+ field) else cls
    }

    def injectMethod(method: DefDef): A = {
      if (!fieldAndMethodNames.contains(method.nameStr)) copyWithBody(body :+ method) else cls
    }

    def mapFields(func: ValDef => ValDef): A = copyWithBody(body.map{
      case v: ValDef => func(v)
      case x => x
    })

    def mapMethods(func: DefDef => DefDef): A = copyWithBody(body.map{
      case d: DefDef if !d.isConstructor => func(d)
      case x => x
    })

    def copyWithTemplate(template: Template): A
    def copyWithBody(body: List[Tree]): A = copyWithTemplate(Template(parents,selfType,body))
    def copyWithParents(parents: List[Tree]): A = copyWithTemplate(Template(parents,selfType,body))

    def mixIn(parent: Tree*): A = {
      if (!parents.cross(parent).exists{case (p,t) => p equalsStructure t})
        copyWithParents(parents :+ parent.head)
      else
        cls
    }
  }

  implicit class ClassOps(val cls: ClassDef) extends Templated[ClassDef] with NamedOps {
    val ClassDef(
      mods:    Modifiers,
      nameType@TypeName(nameStr: String),
      tparams,  // List[Tree]
      template: Template
    ) = cls
    lazy val nameTerm: TermName = nameType.toTermName

    def copyWithTemplate(template: Template): ClassDef = ClassDef(mods,nameType,tparams,template)

    def withVarParams: ClassDef = {
      val params = constructorArgs.head.map(_.name)
      cls.mapFields{
        case field if params.contains(field.name) => field.asVar
        case field => field
      }
    }

    def typeArgs: List[Tree] = tparams.map{tp => Ident(tp.name)}

    def callConstructor(args: Tree*): Tree = {
      methodCall(None, nameStr, List(args.toList), tparams)
    }

    def asCaseClass: ClassDef = ClassDef(mods.withCase,cls.name,tparams,template)
  }

  implicit class ModuleOps(val cls: ModuleDef) extends Templated[ModuleDef] with NamedOps {
    val ModuleDef(
      mods: Modifiers,
      nameTerm @ TermName(nameStr: String),
      template: Template
    ) = cls

    def copyWithTemplate(template: Template): ModuleDef = ModuleDef(mods, nameTerm, template)
  }

  def invalidAnnotationUse(name: String, allowed: String*): Nothing = {
    __c.abort(__c.enclosingPosition, s"@$name annotation can only be used on ${conj(allowed)}")
  }

  implicit class TreeOps(tree: Tree) {
    def asObject: ModuleDef = tree match {
      case md: ModuleDef => md
      case _ => throw new Exception("tree was not an object definition")
    }
    def asClass: ClassDef = tree match {
      case cd: ClassDef => cd
      case _ => throw new Exception("tree was not a class definition")
    }
    def asDef: DefDef = tree match {
      case dd: DefDef => dd
      case _ => throw new Exception("tree was not a def definition")
    }
    def asVal: ValDef = tree match {
      case vd: ValDef => vd
      case _ => throw new Exception("tree was not a val definition")
    }
  }

}

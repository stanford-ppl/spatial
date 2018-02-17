package forge.tags

import forge.utils.conj
import forge.implicits.collections._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

private[forge] case class MacroUtils[Ctx <: blackbox.Context](__c: Ctx) {
  import __c.universe._

  // Fix for bug where <caseaccessor> gets added to (private) implicit fields
  def fieldsFix(fields: List[ValDef]): List[ValDef] = fields.map{
    case ValDef(mods,name,tp,rhs) if mods.hasFlag(Flag.CASEACCESSOR) && mods.hasFlag(Flag.IMPLICIT) && mods.hasFlag(Flag.SYNTHETIC) =>
      val flags = Modifiers(Flag.SYNTHETIC | Flag.IMPLICIT | Flag.PARAMACCESSOR)
      ValDef(flags, name, tp, rhs)
    case v => v
  }

  def makeTypeName(tp: TypeDef): Tree = {
    val TypeDef(_,TypeName(name),targs,_) = tp
    makeType(name, targs)
  }

  def makeType(name: String, targs: List[TypeDef]): Tree = {
    val init = Ident(TypeName(name))
    if (targs.isEmpty) init else AppliedTypeTree(init, targs.map(makeTypeName))
  }

  def makeDefCall(name: String, targs: List[TypeDef], argss: List[List[Tree]]): Tree = {
    val call = Ident(TermName(name))
    val fullCall = if (targs.isEmpty) call else {
      TypeApply(call, targs.map(makeTypeName))
    }
    argss.foldLeft(fullCall){(call,args) => Apply(call,args) }
  }

  def makeType(name: String): Tree = makeType(name, Nil)

  def isWildcardType(tp: Tree, str: String): Boolean = tp match {
    case ExistentialTypeTree(AppliedTypeTree(Ident(TypeName(`str`)), List(Ident(TypeName(arg)))), _) => arg.startsWith("_$")
    case _ => false
  }

  def modifyClassFields(
    cls: ClassDef,
    func: ValDef => ValDef
  ): ClassDef = {
    val ClassDef(mods,TypeName(name),tparams,Template(parents,self,_)) = cls
    val (fieldsX,methods) = cls.fieldsAndMethods
    val fields = fieldsFix(fieldsX)
    val fields2 = fields.map(func)
    val body2 = fields2 ++ methods
    ClassDef(mods,TypeName(name),tparams,Template(parents,self,body2))
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

  implicit class ValDefOps(v: ValDef) extends NamedOps {
    val ValDef(mods,nameTerm@TermName(nameStr),tpTree,rhs) = v

    def tp: Option[Tree] = if (tpTree == EmptyTree) None else Some(tpTree)

    def asVar: ValDef = ValDef(mods.withMutable,nameTerm,tpTree,rhs)

    def hasType(types: Seq[Tree]): Boolean = types.exists{tp => tp equalsStructure tpTree }
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

    def modifyBody(func: Tree => Tree): DefDef = {
      val body2 = func(body)
      DefDef(mods,nameTerm,tparams,paramss,retTp,body2)
    }
  }

  trait TemplateOps[A] {
    def cls: A
    def template: Template
    lazy val Template(
      parents,   // List[Tree]
      selfType,  // ValDef
      __body     // List[Tree] (ValDefs and DefDefs)
    ) = template
    lazy val fields: List[ValDef] = fieldsFix(__body.collect{case x: ValDef => x })
    lazy val methods: List[DefDef] = __body.collect{case x: DefDef => x }
    lazy val body: List[Tree] = fields ++ methods

    def fieldsAndMethods: (List[ValDef],List[DefDef]) = (fields, methods)
    def fieldAndMethodNames: List[String] = fields.map(_.nameStr) ++ methods.map(_.nameStr)

    def constructors: List[DefDef] = methods.filter(_.name == termNames.CONSTRUCTOR)
    def nonConstructorMethods: List[DefDef] = methods.filterNot(_.name == termNames.CONSTRUCTOR)

    def constructor: Option[DefDef] = constructors.headOption
    def constructorArgs: List[List[ValDef]] = constructor.map{d =>  d.paramss }.getOrElse(Nil)

    def injectField(tree: Tree): A = tree match {
      case v: ValDef => injectField(v)
      case _ => __c.abort(__c.enclosingPosition, "Non-field passed to injectField")
    }
    def injectMethod(tree: Tree): A = tree match {
      case m: DefDef => injectMethod(m)
      case _ => __c.abort(__c.enclosingPosition, "Non-method passed to injectMethod")
    }

    def injectField(field: ValDef): A = {
      if (!fieldAndMethodNames.contains(field.nameStr)) copyWithBody(body :+ field) else cls
    }

    def injectMethod(method: DefDef): A = {
      if (!fieldAndMethodNames.contains(method.nameStr)) copyWithBody(body :+ method) else cls
    }

    def mapFields(func: ValDef => ValDef): A = copyWithBody(fields.map(func) ++ methods)
    def mapMethods(func: DefDef => DefDef): A = copyWithBody(constructors ++ fields ++ nonConstructorMethods.map(func))

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

  implicit class ClassOps(val cls: ClassDef) extends TemplateOps[ClassDef] with NamedOps {
    val ClassDef(
      mods:    Modifiers,
      nameType@TypeName(nameStr: String),
      tparams,  // List[Tree]
      template: Template
    ) = cls

    val nameTerm = nameType.toTermName
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
      makeDefCall(nameStr,tparams,List(args.toList))
    }

    def asCaseClass: ClassDef = ClassDef(mods.withCase,cls.name,tparams,template)
  }

  implicit class ModuleOps(val cls: ModuleDef) extends TemplateOps[ModuleDef] with NamedOps {
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

}

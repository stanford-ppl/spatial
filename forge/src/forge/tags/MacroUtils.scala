package forge.tags

import forge.utils.conj

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

  def injectClassMethod(
    cls: ClassDef,
    errorIfExists: Boolean,
    method: (String, Tree) => Tree
  ): ClassDef = {
    val ClassDef(mods,TypeName(name),tparams,Template(parents,self,_)) = cls
    val (fieldsX, methods) = cls.fieldsAndMethods

    val fields = fieldsFix(fieldsX)
    val body = fields ++ methods

    val fieldNames = fields.map(_.name)
    val methodNames = methods.map(_.name)
    val names = fieldNames ++ methodNames
    val tp = makeType(name,tparams)
    val newMethod = method(name,tp)

    val methodName = newMethod match {
      case d: DefDef => d.name
      case _ =>
        __c.abort(__c.enclosingPosition, "Inject method did not return a def.")
    }
    if (!names.contains(methodName)) {
      ClassDef(mods,TypeName(name),tparams,Template(parents,self,body :+ newMethod))
    }
    else if (errorIfExists) {
      __c.error(__c.enclosingPosition, s"Could not inject method $methodName to class - method already defined")
      cls
    }
    else cls
  }

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
    def name: String
    def nameLiteral: Literal = Literal(Constant(name))
  }

  implicit class ValDefOps(v: ValDef) extends NamedOps {
    val ValDef(mods,TermName(name),tpTree,rhs) = v

    def tp: Option[Tree] = if (tpTree == EmptyTree) None else Some(tpTree)

    def asVar: ValDef = ValDef(mods.withMutable,TermName(name),tpTree,rhs)

    def hasType(types: Seq[Tree]): Boolean = types.exists{tp => tp equalsStructure tpTree }
  }
  object Param {
    def impl(name: String, tp: Tree, rhs: Tree = EmptyTree): ValDef = {
      ValDef(Mods.implicitParam, TermName(name), tp, rhs)
    }
  }

  implicit class DefDefOps(df: DefDef) extends NamedOps {
    val DefDef(mods:    Modifiers,
               nameTerm@TermName(name: String),
               tparams, // List[TypeDef],
               paramss, // List[List[ValDef]],
               retTp,   // Tree
               body     // Tree
              ) = df

    def hasImplicits: Boolean = paramss.lastOption.exists(_.exists{_.mods.isImplicit})
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

  trait TemplateOps {
    def template: Template
    lazy val Template(
      parents,   // List[Tree]
      selfType,  // ValDef
      body       // List[Tree] (ValDefs and DefDefs)
    ) = template

    def fieldsAndMethods: (List[ValDef],List[DefDef]) = {
      val fields  = body.collect{case x: ValDef => x }
      val methods = body.collect{case x: DefDef => x }
      (fields,methods)
    }

    def fields: List[ValDef]  = fieldsAndMethods._1
    def methods: List[DefDef] = fieldsAndMethods._2
    def constructors: List[DefDef] = methods.filter(_.name == termNames.CONSTRUCTOR)
    def nonConstructorMethods: List[DefDef] = methods.filterNot(_.name == termNames.CONSTRUCTOR)

    def constructor: Option[DefDef] = constructors.headOption
    def constructorArgs: List[List[ValDef]] = constructor.map{d =>  d.paramss }.getOrElse(Nil)
  }

  implicit class ClassOps(cls: ClassDef) extends TemplateOps with NamedOps {
    val ClassDef(
      mods:    Modifiers,
      nameTerm@TypeName(name: String),
      tparams,  // List[Tree]
      template: Template
    ) = cls

    def injectMethod(method: (String, Tree) => Tree): ClassDef = {
      injectClassMethod(cls, errorIfExists = false, method)
    }
//    def optionalInjectMethod(method: (String,Tree) => Tree): ClassDef = {
//      injectClassMethod(cls, errorIfExists = false, method)
//    }

    def mapFields(func: ValDef => ValDef): ClassDef = {
      modifyClassFields(cls,func)
    }
    def mapMethods(func: DefDef => DefDef): ClassDef = {
      val methods = nonConstructorMethods.map(func)
      ClassDef(mods,nameTerm,tparams,Template(parents,selfType,fields ++ constructors ++ methods))
    }
    def withVarParams: ClassDef = {
      val params = constructorArgs.head.map(_.name)
      cls.mapFields{
        case field if params.contains(field.name) => field.asVar
        case field => field
      }
    }

    def typeArgs: List[Tree] = tparams.map{tp => Ident(tp.name)}

    def callConstructor(args: Tree*): Tree = {
      makeDefCall(name,tparams,List(args.toList))
    }

    def asCaseClass: ClassDef = ClassDef(mods.withCase,cls.name,tparams,template)
  }

  implicit class ModuleOps(mf: ModuleDef) extends TemplateOps with NamedOps {
    val ModuleDef(
      mods: Modifiers,
      nameTerm @ TermName(name: String),
      template: Template
    ) = mf

    def mapMethods(func: DefDef => DefDef): ModuleDef = {
      val methods = nonConstructorMethods.map(func)
      ModuleDef(mods, nameTerm, Template(parents,selfType,fields ++ constructors ++ methods))
    }
  }

  def invalidAnnotationUse(name: String, allowed: String*): Nothing = {
    __c.abort(__c.enclosingPosition, s"@$name annotation can only be used on ${conj(allowed)}")
  }

}

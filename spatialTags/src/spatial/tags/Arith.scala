package spatial.tags

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

import utils.tags.MacroUtils

class Arith[Ctx <: blackbox.Context](override val c: Ctx) extends TypeclassMacro[Ctx](c) {
  import c.universe._

  def implement(cls: ClassDef, obj: ModuleDef, fields: Seq[ValDef]): (ClassDef, ModuleDef) = {
    val utils = new MacroUtils[c.type](c)
    import utils._
    import c.universe._

    val fieldTypes = fields.map(_.tpTree)
    val fieldNames = fields.map(_.name)
    val arithOpt = fieldTypes.map{tp => q"new argon.static.ExpTypeLowPriority(argon.Type[$tp]).getView[spatial.lang.types.Arith]" }
    val arith    = arithOpt.map{bits => q"$bits.get" }
    val fieldPairs = fieldNames.zip(arith)
    val neg = fieldPairs.map{case (name,a) => q"$a.neg(this.$name)"}
    val fieldXs = fieldNames.map{name => q"this.$name" }

    val add = fieldPairs.map{case (name,a) => q"$a.add(this.$name,that.$name)" }
    val sub = fieldPairs.map{case (name,a) => q"$a.sub(this.$name,that.$name)" }
    val mul = fieldPairs.map{case (name,a) => q"$a.mul(this.$name,that.$name)" }
    val div = fieldPairs.map{case (name,a) => q"$a.div(this.$name,that.$name)" }
    val mod = fieldPairs.map{case (name,a) => q"$a.mod(this.$name,that.$name)" }
    val abs = fieldPairs.map{case (name,a) => q"$a.abs(a.$name)" }
    val ceil = fieldPairs.map{case (name,a) => q"$a.ceil(a.$name)" }
    val floor = fieldPairs.map{case (name,a) => q"$a.floor(a.$name)" }
    val clsName = cls.fullName

    val cls2 = {
      cls.mixIn(tq"Arith[$clsName]")
        .injectMethod(
          q"""private def __arith(op: java.lang.String)(func: => ${cls.fullName})(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                 val arithOpt = List(..$arithOpt)
                 if (!arithOpt.forall(_.isDefined) || arithOpt.exists(_ eq null)) {
                    argon.error(ctx, op + " not defined for " + this.tp)
                    argon.error(ctx)
                    argon.err[${cls.fullName}](op + " not defined for " + this.tp)
                 }
                 else func
               }""".asDef)
        .injectMethod(q"""def unary_-()(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("negate"){ ${obj.name}.apply(..$neg) }""".asDef)
        .injectMethod(
               q"""def +(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = {
                      Console.println(List(..$fieldXs))
                      __arith("+"){ ${obj.name}.apply(..$add) }
                   }""".asDef)
        .injectMethod(q"""def -(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("-"){ ${obj.name}.apply(..$sub) }""".asDef)
        .injectMethod(q"""def *(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("*"){ ${obj.name}.apply(..$mul) }""".asDef)
        .injectMethod(q"""def /(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("/"){ ${obj.name}.apply(..$div) }""".asDef)
        .injectMethod(q"""def %(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("%"){ ${obj.name}.apply(..$mod) }""".asDef)
        .injectMethod(q"""def abs(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("abs"){ ${obj.name}.apply(..$abs) }""".asDef)
        .injectMethod(q"""def ceil(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("ceil"){ ${obj.name}.apply(..$ceil) }""".asDef)
        .injectMethod(q"""def floor(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("floor"){ ${obj.name}.apply(..$floor) }""".asDef)
    }
    val obj2 = obj

    (cls2, obj2)
  }
}
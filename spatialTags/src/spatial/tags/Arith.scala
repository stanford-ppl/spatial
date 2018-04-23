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
    val arithOpt   = fieldTypes.map{tp => q"new argon.static.ExpTypeLowPriority(argon.Type[$tp]).getView[spatial.lang.types.Arith]" }
    val arith      = arithOpt.map{a => q"$a.get" }
    val fieldPairs = fieldNames.zip(arith)
    val neg = fieldPairs.map{case (name,a) => q"$a.neg(this.$name(ctx,state))(ctx,state)"}
    val add = fieldPairs.map{case (name,a) => q"$a.add(this.$name(ctx,state),that.$name(ctx,state))(ctx,state)" }
    val sub = fieldPairs.map{case (name,a) => q"$a.sub(this.$name(ctx,state),that.$name(ctx,state))(ctx,state)" }
    val mul = fieldPairs.map{case (name,a) => q"$a.mul(this.$name(ctx,state),that.$name(ctx,state))(ctx,state)" }
    val div = fieldPairs.map{case (name,a) => q"$a.div(this.$name(ctx,state),that.$name(ctx,state))(ctx,state)" }
    val mod = fieldPairs.map{case (name,a) => q"$a.mod(this.$name(ctx,state),that.$name(ctx,state))(ctx,state)" }
    val abs = fieldPairs.map{case (name,a) => q"$a.abs(a.$name(ctx,state))(ctx,state)" }
    val ceil = fieldPairs.map{case (name,a) => q"$a.ceil(a.$name(ctx,state))(ctx,state)" }
    val floor = fieldPairs.map{case (name,a) => q"$a.floor(a.$name(ctx,state))(ctx,state)" }
    val clsName = cls.fullName

    val cls2 = {
      cls.mixIn(tq"Arith[$clsName]")
        .injectMethod(
          q"""private def __arith(op: java.lang.String)(func: => ${cls.fullName})(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                 val arithOpt = List(..$arithOpt)
                 if (!arithOpt.forall(_.isDefined) || arithOpt.exists(_ eq null)) {
                    argon.error(ctx, op + " not defined for " + this.tp)(state)
                    argon.error(ctx)(state)
                    argon.err[${cls.fullName}](op + " not defined for " + this.tp)(argon.Type[${cls.fullName}],state)
                 }
                 else func
               }""".asDef)
        .injectMethod(q"""def unary_-()(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("negate"){ ${obj.name}.apply(..$neg)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def +(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("+"){ ${obj.name}.apply(..$add)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def -(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("-"){ ${obj.name}.apply(..$sub)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def *(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("*"){ ${obj.name}.apply(..$mul)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def /(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("/"){ ${obj.name}.apply(..$div)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def %(that: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("%"){ ${obj.name}.apply(..$mod)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def abs(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("abs"){ ${obj.name}.apply(..$abs)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def ceil(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("ceil"){ ${obj.name}.apply(..$ceil)(ctx,state) }(ctx,state)""".asDef)
        .injectMethod(q"""def floor(a: $clsName)(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = __arith("floor"){ ${obj.name}.apply(..$floor)(ctx,state) }(ctx,state)""".asDef)
    }
    val obj2 = obj

    (cls2, obj2)
  }
}
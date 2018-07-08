package argon.tags

import utils.tags.MacroUtils

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class Bits[Ctx <: blackbox.Context](override val c: Ctx) extends TypeclassMacro[Ctx](c) {
  import c.universe._

  def implement(cls: ClassDef, obj: ModuleDef, fields: Seq[ValDef]): (ClassDef, ModuleDef) = {
    val utils = new MacroUtils[c.type](c)
    import c.universe._
    import utils._

    val fieldTypes = fields.map(_.tpTree)
    val fieldNames = fields.map(_.name)
    val bitssOpt = fieldTypes.map{tp => q"new argon.static.ExpTypeLowPriority(argon.Type[$tp]).getView[argon.lang.types.Bits]" }
    val bitss  = bitssOpt.map{bits => q"$bits.get" }
    val nbitss = bitss.map{bits => q"$bits.nbits(ctx,state)" }
    val zeros  = bitss.map{bits => q"$bits.zero(ctx,state)" }
    val ones   = bitss.map{bits => q"$bits.one(ctx,state)" }
    val maxes  = fieldNames.zip(bitss).map{case (name,bits) => q"$bits.random(max.map(_.$name(ctx,state)))(ctx,state)"}
    val clsName = cls.fullName

    val cls2 = {
      cls.mixIn(tq"Bits[$clsName]")
         .injectMethod(
           q"""private def bitsCheck(op: java.lang.String)(func: => ${cls.fullName})(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                 val bitsOpt = List(..$bitssOpt)
                 if (!bitsOpt.forall(_.isDefined)) {
                    argon.error(ctx, s"$$op not defined for $${this.tp}")(state)
                    argon.error(ctx)(state)
                    argon.err[${cls.fullName}](s"$$op not defined for $${this.tp}")(argon.Type[${cls.fullName}],state)
                 }
                 else func
               }""".asDef)

         .injectMethod(
           q"""def nbits(implicit ctx: forge.SrcCtx, state: argon.State): scala.Int = {
                 val bitsOpt = List(..$bitssOpt)
                 if (!bitsOpt.forall(_.isDefined)) {
                   argon.error(ctx, s"nbits is not defined for $${this.tp}")(state)
                   argon.error(ctx)(state)
                   0
                 }
                 else List(..$nbitss).sum
               }""".asDef)
         .injectMethod(q"""def zero(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck("zero"){ ${obj.name}.apply(..$zeros)(ctx,state) }(ctx,state)""".asDef)
         .injectMethod(q"""def one(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck("one"){ ${obj.name}.apply(..$ones)(ctx,state) }(ctx,state)""".asDef)
         .injectMethod(q"""def random(max: Option[$clsName])(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck("random"){ ${obj.name}.apply(..$maxes)(ctx,state) }(ctx,state)""".asDef)
    }
    val obj2 = obj

    (cls2, obj2)
  }
}

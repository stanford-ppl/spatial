package spatial.tags

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

import utils.tags.MacroUtils

class Bits[Ctx <: blackbox.Context](override val c: Ctx) extends TypeclassMacro[Ctx](c) {
  import c.universe._

  def implement(cls: ClassDef, obj: ModuleDef, fields: Seq[ValDef]): (ClassDef, ModuleDef) = {
    val utils = new MacroUtils[c.type](c)
    import utils._
    import c.universe._

    val fieldTypes = fields.map(_.tpTree)
    val fieldNames = fields.map(_.name)
    val bitssOpt = fieldTypes.map{tp => q"new argon.static.ExpTypeLowPriority(argon.Type[$tp]).getView[spatial.lang.types.Bits]" }
    val bitss  = bitssOpt.map{bits => q"$bits.get" }
    val nbitss = bitss.map{bits => q"$bits.nbits" }
    val zeros  = bitss.map{bits => q"$bits.zero" }
    val ones   = bitss.map{bits => q"$bits.one" }
    val maxes  = fieldNames.zip(bitss).map{case (name,bits) => q"$bits.random(max.map(_.$name))"}
    val clsName = cls.fullName

    val cls2 = {
      cls.mixIn(tq"Bits[$clsName]")
         .injectMethod(q"""private def bitsCheck(): scala.Unit = {
                             val bitsOpt = List(..$bitssOpt)
                             if (!bitsOpt.forall(_.isDefined)) throw new Exception("Bits not defined for " + this.tp)
                           }""".asDef)
         .injectMethod(
           q"""private def bitsCheck2(op: java.lang.String)(func: => ${cls.fullName})(implicit ctx: forge.SrcCtx, state: argon.State): ${cls.fullName} = {
                 val bitsOpt = List(..$bitssOpt)
                 if (!bitsOpt.forall(_.isDefined)) {
                    argon.error(ctx, op + " not defined for " + this.tp)
                    argon.error(ctx)
                    argon.err[${cls.fullName}](op + " not defined for " + this.tp)
                 }
                 else func
               }""".asDef)

         .injectMethod(q"""def nbits(implicit ctx: forge.SrcCtx, state: argon.State): scala.Int = { bitsCheck(); List(..$nbitss).sum }""".asDef)
         .injectMethod(q"""def zero(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck2("zero"){ ${obj.name}.apply(..$zeros) }""".asDef)
         .injectMethod(q"""def one(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck2("one"){ ${obj.name}.apply(..$ones) }""".asDef)
         .injectMethod(
            q"""def random(max: Option[$clsName])(implicit ctx: forge.SrcCtx, state: argon.State): $clsName = bitsCheck2("random"){
                  ${obj.name}.apply(..$maxes)
                }""".asDef)
    }
    val obj2 = obj

    (cls2, obj2)
  }
}

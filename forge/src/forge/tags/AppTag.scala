package forge.tags

import language.experimental.macros
import scala.reflect.macros.blackbox

class AppTag(dsl: String, dslApp: String) {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new Virtualizer[c.type](c)
    import util._
    import c.universe._

    val appType = Ident(TypeName(dslApp))
    val inputs = annottees.toList
    val outputs: List[Tree] = inputs match {
      case (a:ValDef) :: as if !a.mods.hasFlag(Flag.PARAM) =>
        runVirtualizer(a) ::: as
      case (a:DefDef) :: as =>
        runVirtualizer(a) ::: as
      case (a:ClassDef) :: as =>
        val mod = a.mixIn(appType)
        runVirtualizer(mod) ::: as
      case (a:ModuleDef) :: as =>
        val mod = a.mixIn(appType)
        runVirtualizer(mod) ::: as
      case _ => invalidAnnotationUse(dsl, "classes", "objects", "traits", "defs")
    }

    //info(showCode(outputs.head))

    q"..$outputs"
  }
}

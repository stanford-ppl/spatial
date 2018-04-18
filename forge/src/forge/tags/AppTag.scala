package forge.tags

import language.experimental.macros
import scala.reflect.macros.blackbox

class AppTag(dsl: String, dslApp: String) {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new Virtualizer[c.type](c)
    import util._
    import c.universe._

    val appType = Ident(TypeName(dslApp))
    // Bit strange here. Virtualization requires at least EmbeddedControls to have
    // an implementation default. However, we also can't mix EmbeddedControls in directly
    // because we need a view of the DSL's overrides
    val inputs = annottees.toList
    val outputs = inputs match {
      case (a:ClassDef) :: as =>
        val mod = a.mixIn(appType) //.injectStm(q"import ${TermName(dsl)}.dsl._")
                   .renameMethod("main", "entry")
        virt(mod) ::: as
      case (a:ModuleDef) :: as =>
        val mod = a.mixIn(appType) //.injectStm(q"import ${TermName(dsl)}.dsl._")
                   .renameMethod("main", "entry")
        virt(mod) ::: as
      case _ => invalidAnnotationUse(dsl, "classes", "objects", "traits")
    }

    //info(showCode(outputs.head))

    q"..$outputs"
  }
}

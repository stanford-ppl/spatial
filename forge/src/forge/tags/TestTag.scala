package forge.tags

import language.experimental.macros
import scala.reflect.macros.blackbox

class TestTag(dsl: String, dslTest: String) {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new Virtualizer[c.type](c)
    import util._
    import c.universe._

    val testType = Ident(TypeName(dslTest))
    // Bit strange here. Virtualization requires at least EmbeddedControls to have
    // an implementation default. However, we also can't mix EmbeddedControls in directly
    // because we need a view of the DSL's overrides
    val inputs = annottees.toList
    val outputs = inputs match {
      case (a:ClassDef) :: as =>
        val mod = a.mixIn(testType).injectMethod(q"override def toString: java.lang.String = this.name".asDef)
        val cls = virt(mod)
        cls ::: as

      case _ => invalidAnnotationUse("test", "classes", "traits")
    }

    //outputs.foreach{out => info(showCode(out)) }

    q"..$outputs"
  }
}


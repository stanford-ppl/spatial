package forge.tags

import language.experimental.macros
import scala.reflect.macros.blackbox

class TestTag(dsl: String, dslTest: String, dslApp: String, verbose: Boolean = false) {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new Virtualizer[c.type](c)
    import util._
    import c.universe._

    val testType = Ident(TypeName(dslTest))
    val appType  = Ident(TypeName(dslApp))
    val inputs = annottees.toList
    val outputs = inputs match {
      case (a:ClassDef) :: as =>
        val mod = a.mixIn(testType)
                   .injectMethod(q"override def toString: java.lang.String = this.name".asDef)
                   .renameMethod("main", "entry")

        val cls = virt(mod).head.asClass
        val obj = q"object ${cls.name.toTermName} extends ${cls.name} with $appType".asObject

        if (verbose) info(showCode(cls))

        List(cls,obj) ::: as

      case _ => invalidAnnotationUse("test", "classes", "traits")
    }

    q"..$outputs"
  }
}


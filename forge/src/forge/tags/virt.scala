package forge.tags

import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox

/** Annotation class for @virt macro annotation. */
final class virt extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro virt.impl
}

/** Companion object implementing @virt macro annotation. */
private object virt {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val util = new Virtualizer[c.type](c)
    import util._
    import c.universe._

    /* The first element of `annottee` is the one actually carrying the
     * annotation.  The rest are owners/companions (class, method,
     * object, etc.), and we don't want to stage them.
     *
     * Also, for now, we don't stage annotated type, class or
     * method parameters (this may change in the future).
     */
    val inputs = annottees.toList
    val outputs = inputs match {
      case (a:ValDef) :: as if a.mods.hasFlag(Flag.PARAM) =>
        c.warning(c.enclosingPosition, "@virt cannot be used on parameters.")
        inputs
      case (_:TypeDef) :: as =>
        c.warning(c.enclosingPosition, "@virt cannot be used on type aliases.")
        inputs

      case a :: as => virtualize(a) ::: as
      case Nil     => Nil
    }

    // c.info(c.enclosingPosition, showCode(expandees.head), true)

    q"..$outputs"
  }
}

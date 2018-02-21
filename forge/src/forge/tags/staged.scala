package forge.tags

import language.experimental.macros
import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox

/** Annotation class for @staged macro annotation. */
final class staged extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro staged.impl
}

/** Companion object implementing @staged macro annotation. */
private object staged {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    import c.universe._

    /* Create a transformer for virtualization. */
    val util = new Virtualizer[c.type](c)
    import util._

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
        c.warning(c.enclosingPosition, "@staged cannot be used on parameters.")
        inputs
      case (_:TypeDef) :: as =>
        c.warning(c.enclosingPosition, "@staged cannot be used on type aliases.")
        inputs

      case a :: as => virtualize(a) ::: as
      case Nil     => Nil
    }

    // c.info(c.enclosingPosition, showCode(expandees.head), true)

    q"..$outputs"
  }
}

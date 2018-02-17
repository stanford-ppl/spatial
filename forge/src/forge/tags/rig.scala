package forge.tags

import scala.annotation.StaticAnnotation
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

final class rig extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro rig.impl
}

object rig {
  def impl(c: blackbox.Context)(annottees: c.Tree*): c.Tree = {
    val withCtx   = ctx.impl(c)(annottees:_*)
    val withState = stateful.impl(c)(withCtx)
    withState
  }
}
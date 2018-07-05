package pir

import pir.lang.static.ExternalStatics

object dsl extends ExternalStatics {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @pir macro annotation. */
  final class pir extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro pir.impl
  }
  private object pir extends AppTag("pir", "PIRApp")

}

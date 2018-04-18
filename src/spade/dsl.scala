package spade

import spade.lang.static.ExternalStatics

object dsl extends ExternalStatics {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.{AppTag,TestTag}

  /** Annotation class for @spade macro annotation. */
  final class spade extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spade.impl
  }
  private object spade extends AppTag("spade", "SpadeDesign")

  final class test extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro test.impl
  }
  private object test extends TestTag("spade", "SpadeTest", "SpadeDesign")
}

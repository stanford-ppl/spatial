package spatial

object dsl extends lang.static.ExternalStatics {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @spatial macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

}

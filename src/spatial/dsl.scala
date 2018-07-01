package spatial

trait SpatialDSL extends lang.api.StaticAPI_Frontend

object libview extends SpatialDSL {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @spatial macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

  final class struct extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro tags.StagedStructsMacro.impl
  }
}

object dsl extends SpatialDSL with lang.api.StaticAPI_Shadowing {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @spatial macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

  final class struct extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro tags.StagedStructsMacro.impl
  }
}

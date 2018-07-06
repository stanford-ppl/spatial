package spatial

import argon.tags.StagedStructsMacro

trait SpatialDSL extends lang.api.StaticAPI_Frontend

/** A "library" view of the Spatial DSL without any Scala name shadowing. */
object libdsl extends SpatialDSL {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @spatial macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

  final class struct extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro StagedStructsMacro.impl
  }
}

/** A full view of the Spatial DSL, including shadowing of Scala names. */
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
    def macroTransform(annottees: Any*): Any = macro StagedStructsMacro.impl
  }
}

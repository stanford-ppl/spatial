package spatial

trait SpatialDSL extends lang.static.FrontendStatics

object libview extends SpatialDSL {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.AppTag

  /** Annotation class for @test macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

  final class struct extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro tags.StagedStructsMacro.impl
  }
}

object dsl extends SpatialDSL with lang.static.ShadowingStatics {
  import language.experimental.macros
  import scala.annotation.StaticAnnotation
  import forge.tags.{AppTag,TestTag}

  /** Annotation class for @test macro annotation. */
  final class spatial extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro spatial.impl
  }
  private object spatial extends AppTag("spatial", "SpatialApp")

  final class test extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro test.impl
  }
  private object test extends TestTag("spatial", "SpatialTest", "SpatialApp")

  final class struct extends StaticAnnotation {
    def macroTransform(annottees: Any*): Any = macro tags.StagedStructsMacro.impl
  }
}

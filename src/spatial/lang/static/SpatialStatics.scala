package spatial.lang
package static

trait SpatialStatics extends Implicits with utils.Overloads with SpatialVirtualization
  with StaticBits
  with StaticFileIO
  with StaticMath
  with StaticMisc
  with StaticTransfers
  with StaticTuples
  with Constructors

/** Internal view of Spatial */
trait InternalStatics extends SpatialStatics with InternalAliases

/** External view for extending DSLs */
trait ExternalStatics extends InternalStatics with ExternalAliases

/** Application view */
trait FrontendStatics extends ExternalStatics
  with StaticDebuggingExternal
{
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
}

trait ShadowingStatics extends FrontendStatics with ShadowingAliases
package spatial.lang
package static

trait SpatialStatics extends Lifts with Casts with utils.Overloads
  with StaticBits
  with StaticMath
  with StaticMisc
  with StaticTransfers

/** Internal view of Spatial **/
trait InternalStatics extends SpatialStatics with InternalAliases

/** External view for extending DSLs **/
trait ExtensionStatics extends InternalStatics with ExternalAliases

/** Application view **/
trait ExternalStatics extends ExtensionStatics with SpatialVirtualization
  with StaticDebugsExternal
{
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
}
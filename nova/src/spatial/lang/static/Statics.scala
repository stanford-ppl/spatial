package spatial.lang.static

trait Statics extends Types
  with BitsMethods
  with Strings
  with Overloads
  with Numerics
  with Maths
  with Ints

/** Internal view of Spatial **/
trait InternalStatics extends Statics with InternalAliases

/** External view for extending DSLs **/
trait ExtensionStatics extends InternalStatics with ExternalAliases

/** Application view **/
trait ExternalStatics extends ExtensionStatics
  with Debugs
{
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
}
package spatial.lang.api

import argon.lang.api.{BitsAPI, TuplesAPI}
import spatial.lang.{ExternalAliases, InternalAliases, ShadowingAliases}

/** Internal view of Spatial */
trait StaticAPI extends Implicits with utils.Overloads with SpatialVirtualization
  with ArrayAPI
  with TensorConstructorAPI
  with BitsAPI
  with MuxAPI
  with FileIOAPI
  with MathAPI
  with MiscAPI
  with TransferAPI
  with TuplesAPI
  with UserData
  with ControlAPI {
  this: InternalAliases =>
}

trait StaticAPI_Internal extends StaticAPI with InternalAliases
  with DebuggingAPI_Internal

/** External view for extending DSLs */
trait StaticAPI_External extends StaticAPI_Internal with ExternalAliases

/** Application view */
trait StaticAPI_Frontend extends StaticAPI_External
{
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
  type Cast[A,B] = argon.Cast[A,B]
}

trait StaticAPI_Shadowing extends StaticAPI_Frontend with ShadowingAliases
  with DebuggingAPI_Shadowing
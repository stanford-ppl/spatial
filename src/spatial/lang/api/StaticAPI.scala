package spatial.lang.api

import argon.lang.api.{BitsAPI, TuplesAPI}
import spatial.lang.{ExternalAliases, InternalAliases, ShadowingAliases}

/** Internal view of Spatial */
trait StaticAPI_Internal extends InternalAliases with SpatialVirtualization with Implicits
  with utils.Overloads
  with ArrayAPI
  with BitsAPI
  with ControlAPI
  with DebuggingAPI_Internal
  with FileIOAPI
  with MathAPI
  with MiscAPI
  with MuxAPI
  with ShuffleAPI
  with TensorConstructorAPI
  with TransferAPI
  with TuplesAPI
  with UserData

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

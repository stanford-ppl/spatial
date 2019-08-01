package fringe.templates.euresys

import chisel3._
import chisel3.util._
import fringe.utils._
import fringe.Ledger._
import scala.collection.mutable._

// CoaXPress Image Header / Metadata record
class Metadata_rec() extends Bundle {
  val StreamId = UInt(8.W)
  val SourceTag = UInt(16.W)
  val Xsize = UInt(24.W)
  val Xoffs = UInt(24.W)
  val Ysize = UInt(24.W)
  val Yoffs = UInt(24.W)
  val DsizeL = UInt(24.W)
  val PixelF = UInt(16.W)
  val TapG = UInt(16.W)
  val Flags = UInt(8.W)
  val Timestamp = UInt(32.W)
  val PixProcessingFlgs = UInt(8.W)
  val Status = UInt(32.W)

  override def cloneType(): this.type = new Metadata_rec().asInstanceOf[this.type]
}

class CXPStream() extends Bundle {
  val TDATA = UInt(256.W)
  val TUSER = UInt(4.W)

  override def cloneType(): this.type = new CXPStream().asInstanceOf[this.type]
}
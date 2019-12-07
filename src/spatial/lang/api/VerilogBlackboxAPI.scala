package spatial.lang.api

import argon._
import forge.tags._
import spatial.metadata.blackbox._
import spatial.node.VerilogBlackbox

trait VerilogBlackboxAPI {
  /** Instantiate a verilog black box.  This assumes the blackbox has "clock" and "reset" input ports in the verilog
    * and the programmer does not need to explicitly declare or wire these in Spatial.  Assumes there is no "enable" signal
    * in the verilog module (see issue #287).  You must provide the full path to the verilog file, fixed latency for the module,
    * and whether or not the module can run pipelined (i.e is II = 1 ok?).
    */
  @api def verilogBlackBox[A:Struct,B:Struct](inputs: Bits[A])(file: String, latency: scala.Int = 1, pipelined: scala.Boolean = true, params: Map[String, Any] = Map()): B = {
    val vbbox = stage(VerilogBlackbox[A,B](inputs))
    vbbox.asInstanceOf[Sym[_]].bboxInfo = BlackboxConfig(file, latency, pipelined, params)
    vbbox
  }
}

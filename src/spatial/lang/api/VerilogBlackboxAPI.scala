package spatial.lang.api

import argon._
import forge.tags._
import spatial.metadata.blackbox._
import spatial.metadata.control._
import spatial.node.{VerilogBlackbox, VerilogCtrlBlackbox}

trait VerilogBlackboxAPI {
  /** Instantiate a verilog black box as a primitive node.  This assumes the blackbox has "clock" and "reset" input ports in the verilog
    * and the programmer does not need to explicitly declare or wire these in Spatial.  Assumes there is no "enable" signal
    * in the verilog module (see issue #287).  You must provide the full path to the verilog file, fixed latency for the module,
    * and whether or not the module can run pipelined (i.e is II = 1 ok?).
    * If module name you are invoking differs from the [name].v part of the file path, then you can provide Option[String] as the module name
    */
  @api def verilogPrimitiveBlackbox[A:Struct,B:Struct](inputs: Bits[A])(file: String, moduleName: Option[String] = None, latency: scala.Int = 1, pipelined: scala.Boolean = true, params: Map[String, Any] = Map()): B = {
    val vbbox = stage(VerilogBlackbox[A,B](inputs))
    vbbox.asInstanceOf[Sym[_]].bboxInfo = BlackboxConfig(file, moduleName, latency, pipelined, params)
    vbbox
  }

  /** Instantiate a verilog black box as a controller node.  You must provide the full path to the verilog file.  If the
    *     module name you are invoking differs from the [name].v part of the file path, then you can provide Option[String] as
    *     the module name
    *   Spatial assumes the verilog module has at least the following ports:
    *   - clock (input)
    *   - enable (input)
    *   - reset (input)
    *   - [input]_valid (input) and [input]_ready (output) per input
    *   - [output]_ready (output) and [output]_valid (output) per output
    *   - ready_downstream (input)
    *   - done (output)
    *   This kind of black box is treated as an inner controller and must be the immediate child of a Stream controller.
    */
  @api def verilogControllerBlackbox[A:StreamStruct,B:StreamStruct](inputs: Bits[A])(file: String, moduleName: Option[String] = None, params: Map[String, Any] = Map()): B = {
    val vbbox = stage(VerilogCtrlBlackbox[A,B](inputs))
    vbbox.asInstanceOf[Sym[_]].bboxInfo = BlackboxConfig(file, moduleName, 1, true, params)
    vbbox.asInstanceOf[Sym[_]].rawLevel = Inner
//    assert(vbbox.asInstanceOf[Sym[_]].parent.s.get.getRawSchedule.contains(Stream) && vbbox.asInstanceOf[Sym[_]].parent.s.get.isSingleControl)
    vbbox
  }
}

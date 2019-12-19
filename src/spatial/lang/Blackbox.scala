package spatial.lang

import argon._
import forge.tags._
import spatial.metadata.blackbox.BlackboxConfig
import spatial.node._
import spatial.metadata.blackbox._
import spatial.metadata.control._


@ref class SpatialBlackbox[A:Struct, B:Struct] extends Top[SpatialBlackbox[A,B]] with Ref[scala.Array[Any],SpatialBlackbox[A,B]] {
//  val A: Bits[A] = Bits[A]
  override val __neverMutable = true

  @api def apply(in: A): B = stage(SpatialBlackboxUse[A,B](this, in))
}

@ref class SpatialCtrlBlackbox[A:StreamStruct, B:StreamStruct] extends Top[SpatialCtrlBlackbox[A,B]] with Ref[scala.Array[Any],SpatialCtrlBlackbox[A,B]] {
//  val A: Bits[A] = Bits[A]
  override val __neverMutable = true

  @api def apply(in: A): B = stage(SpatialCtrlBlackboxUse[A,B](this, in))
}

object Blackbox {
  /** Instantiate a Spatial black box as a primitive node.
    *
    * TODO: Allow metaprogrammed params
    */
  @api def SpatialPrimitive[A:Struct,B:Struct](func: A => B): SpatialBlackbox[A,B] = {
    val in = boundVar[A]
    val block = stageLambda1[A, B](in) {
      func(in)
    }
    val sbbox = stageWithFlow(SpatialBlackboxImpl[A, B](block)){box => }
    //    SpatialBlackboxes += sbbox
    sbbox
  }

    /** Instantiate a Spatial black box as a primitive node.
    *
    * TODO: Allow metaprogrammed params
    */
  @api def SpatialController[A:StreamStruct,B:StreamStruct](func: A => B): SpatialCtrlBlackbox[A,B] = {
    val in = boundVar[A]
    val block = stageLambda1[A, B](in) {
      func(in)
    }
    val sbbox = stageWithFlow(SpatialCtrlBlackboxImpl[A, B](block)){box => }
    //    SpatialBlackboxes += sbbox
    sbbox
  }

  /** Instantiate a verilog black box as a primitive node.  This assumes the blackbox has "clock" and "reset" input ports in the verilog
    * and the programmer does not need to explicitly declare or wire these in Spatial.  Assumes there is no "enable" signal
    * in the verilog module (see issue #287).  You must provide the full path to the verilog file, fixed latency for the module,
    * and whether or not the module can run pipelined (i.e is II = 1 ok?).
    * If module name you are invoking differs from the [name].v part of the file path, then you can provide Option[String] as the module name
    */
  @api def VerilogPrimitive[A: Struct, B: Struct](inputs: Bits[A])(file: String, moduleName: Option[String] = None, latency: scala.Int = 1, pipelined: scala.Boolean = true, params: Map[String, Any] = Map()): B = {
    val vbbox = stage(VerilogBlackbox[A, B](inputs))
    vbbox.asInstanceOf[Sym[_]].bboxInfo = BlackboxConfig(file, moduleName, latency, pipelined, params)
    vbbox
  }


  /** Instantiate a verilog black box as a controller node.  You must provide the full path to the verilog file.  If the
    * module name you are invoking differs from the [name].v part of the file path, then you can provide Option[String] as
    * the module name
    * Spatial assumes the verilog module has at least the following ports:
    *   - clock (input)
    *   - enable (input)
    *   - reset (input)
    *   - [input]_valid (input) and [input]_ready (output) per input
    *   - [output]_ready (output) and [output]_valid (output) per output
    *   - ready_downstream (input)
    *   - done (output)
    * This kind of black box is treated as an inner controller and must be the immediate child of a Stream controller.
    */
  @api def VerilogController[A: StreamStruct, B: StreamStruct](inputs: Bits[A])(file: String, moduleName: Option[String] = None, params: Map[String, Any] = Map()): B = {
    val vbbox = stage(VerilogCtrlBlackbox[A, B](inputs))
    vbbox.asInstanceOf[Sym[_]].bboxInfo = BlackboxConfig(file, moduleName, 1, true, params)
    vbbox.asInstanceOf[Sym[_]].rawLevel = Inner
    vbbox
  }

  /**
    * Declares a black box for matrix multiplication with inputs a and b, output c.
    * Output is computed between [i,i+lenI), [j,j+lenJ)
    */
  @api def GEMM[T:Num](
    y: SRAM2[T],
    a: SRAM2[T],
    b: SRAM2[T],
    c: T,
    alpha: T,
    beta: T,
    i: I32,
    j: I32,
    k: I32, // common dimension length
    mt: I32,
    nt: I32
  ): Void = {
    val PP: I32 = 1 (1 -> 16)
    val ctrP = 1 until k par PP
    val cchain = CounterChain(Seq(ctrP))
    val iters = Seq(boundVar[I32])
    stage(GEMMBox(cchain,y,a,b,c,alpha,beta,i,j,mt,nt,iters))
  }

  @api def GEMV: Void = ???
  @api def CONV: Void = ???
  @api def SHIFT(validAfter: Int): Void = ???
}

//
//@ref class SpatialBlackbox[A:Struct,B:Struct] extends Ref[A,B] {
////  override val box = implicitly[Matrix[A] <:< Struct[Matrix[A]]]
//  val A: Type[A] = Type[A]
//  val B: Type[B] = Type[B]
//
//  /** Returns the dimensions of this Matrix. */
//  @api def apply(in: A): B = {
//
//  }
//
//}



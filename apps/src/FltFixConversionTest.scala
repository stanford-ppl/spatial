import spatial.dsl._

@spatial object FltFixConversionTest extends SpatialApp {
  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _16, _16]
    type T_ = FixPt[TRUE, _6, _26]
    type TT = FixPt[TRUE, _6, _31]
    type TT_ = FixPt[TRUE, _10, _54]
    type TTT = FixPt[TRUE, _64, _64]
    type TTT_ = FixPt[TRUE, _10, _128]
    type TTTT = FixPt[TRUE, _128, _128]
    type TTTT_ = FixPt[TRUE, _10, _256]

    type F = Float
    val nArgs = 9

    val inArg = ArgIn[F]
    val outArgs = List.tabulate(nArgs)(_ => ArgOut[F])
    setArg(inArg, args(0).to[F])


    // It seems that a raw conversion might just work if we do F.to[UInt_],
    // since this invokes the prebuilt chisel function directly. Then, we can
    // re-cast the generated value as a fixpoint type... Don't really have an
    // easy fix within the compiler for now...
    Accel {
      outArgs(0) := inArg.value.to[T].to[F]
      outArgs(1) := inArg.value.to[T_].to[F]
      outArgs(2) := inArg.value.to[TT].to[F]
      outArgs(3) := inArg.value.to[TT_].to[F]
      outArgs(4) := inArg.value.to[TTT].to[F]
      outArgs(5) := inArg.value.to[TTT_].to[F]
      outArgs(6) := inArg.value.to[TTTT].to[F]
      outArgs(7) := inArg.value.to[TTTT_].to[F]
      outArgs(8) := inArg.value
    }

    println("T outArg = " + getArg(outArgs(0)))
    println("T_ outArg = " + getArg(outArgs(1)))
    println("TT outArg = " + getArg(outArgs(2)))
    println("TT_ outArg = " + getArg(outArgs(3)))
    println("TTT outArg = " + getArg(outArgs(4)))
    println("TTT_ outArg = " + getArg(outArgs(5)))
    println("TTTT outArg = " + getArg(outArgs(6)))
    println("TTTT_ outArg = " + getArg(outArgs(7)))
    println("Raw data = " + getArg(outArgs(8)))
  }
}

package fringe.templates.math

import chisel3._
import fringe._
import fringe.utils.implicits._

import scala.collection.mutable.Set

class fix2fixBox(s1: Boolean, d1: Int, f1: Int, s2: Boolean, d2: Int, f2: Int, rounding: RoundingMode, saturating: OverflowMode) extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt((d1+f1).W))
    val expect_pos = Input(Bool())
    val expect_neg = Input(Bool())
    val b = Output(UInt((d2+f2).W))
  })

  val sign_extend = true // TODO: What cases do we want this false?

  val has_frac = f2 > 0
  val has_dec = d2 > 0
  val up_frac = f2 max 1
  val up_dec = d2 max 1
  val tmp_frac = Wire(UInt(up_frac.W))
  val new_frac = Wire(UInt(up_frac.W))
  val new_dec = Wire(UInt(up_dec.W))
  if (!has_frac) tmp_frac := 0.U(1.W)
  if (!has_frac) new_frac := 0.U(1.W)
  if (!has_dec)  new_dec := 0.U(1.W)

  // Compute new frac part
  val shave_f = f1 - f2
  val shave_d = d1 - d2
  if (has_frac) {
    if (f2 < f1) { // shrink decimals
      rounding match {
        case Truncate => tmp_frac := io.a(shave_f + f2 - 1, shave_f)
        case Unbiased =>
          val prng = Module(new PRNG(scala.math.abs(scala.util.Random.nextInt)))
          prng.io.en := true.B
          val salted = io.a + prng.io.output(shave_f - 1, 0)
          tmp_frac := salted(shave_f + f2 - 1, shave_f)
      }
    }
    else if (f2 > f1) { // expand decimals
      val expand = f2 - f1
      if (f1 > 0) tmp_frac := util.Cat(io.a(f1 - 1, 0), 0.U(expand.W))
      else           tmp_frac := 0.U(expand.W)
    }
    else { // keep same
      tmp_frac := io.a(f2 - 1, 0)
    }
  }

  // Compute new dec part (concatenated with frac part from before)
  if (has_dec) {
    if (d2 < d1) { // shrink decimals
      saturating match {
        case Wrapping =>
          // dst.debug_overflow := (0 until shave_d).map{i => io.a(d1 + f1 - 1 - i) }.reduce{_||_}
          new_frac := tmp_frac
          new_dec := io.a(d2 + f1 - 1, f1)
        case Saturating =>
          val sign = io.a.msb
          val overflow = (sign & io.expect_pos) | (!sign & io.expect_neg)
          val not_saturated = (io.a(f1 + d1 - 1, f1 + d1 - 1 - shave_d) === 0.U(shave_d.W)) | (~io.a(f1 + d1 - 1, f1 + d1 - 1 - shave_d) === 0.U(shave_d.W))

          val saturated_frac = Mux(io.expect_pos,
            util.Cat(util.Fill(up_frac, true.B)),
            Mux(io.expect_neg, 0.U(up_frac.W), 0.U(up_frac.W)))
          val saturated_dec = Mux(io.expect_pos,
            util.Cat(~(s2 | s1).B, util.Fill(up_dec - 1, true.B)),
            Mux(io.expect_neg, 1.U((d2).W) << (d2 - 1), 1.U((d2).W) << (d2 - 1)))

          new_frac := Mux(io.a === 0.U, 0.U, Mux(not_saturated & !overflow, tmp_frac, saturated_frac))
          new_dec := Mux(io.a === 0.U, 0.U, Mux(not_saturated & !overflow, io.a(d2 + f1 - 1, f1), saturated_dec))
      }
    }
    else if (d2 > d1) { // expand decimals
      val expand = d2 - d1
      val sgn_extend: Bool = if (s1 & sign_extend) io.a.msb else false.B
      new_frac := tmp_frac
      new_dec  := util.Cat(util.Fill(expand, sgn_extend), io.a(f1 + d1 - 1, f1))
    }
    else { // keep same
      new_frac := tmp_frac
      new_dec := io.a(f1 + d1 - 1, f1)
      // (0 until d2).map{ i => number(i + f)*scala.math.pow(2,i).toInt.U }.reduce{_+_}
    }

  }

  if (has_dec & has_frac)       io.b := chisel3.util.Cat(new_dec, new_frac)
  else if (has_dec & !has_frac) io.b := new_dec
  else if (!has_dec & has_frac) io.b := tmp_frac
}


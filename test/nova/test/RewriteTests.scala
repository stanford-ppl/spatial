package nova.test

import spatial.dsl._
import utest._

import spatial.node.FixInv

@spatial object BasicRewriteChecks extends Requirements {
  def main(): Void = {
    val b = random[Bit]
    val t = true.to[Bit]
    val f = false.to[Bit]

    req(!t, f, "Constant prop. failed for Not")
    req(!f, t, "Constant prop. failed for Not")
    req(t || t, t, "Constant prop. failed for Or")
    req(f || t, t, "Constant prop. failed for Or")
    req(t && f, f, "Constant prop. failed for And")
    req(t && t, t, "Constant prop. failed for And")
    req(t !== f, t, "Constant prop. failed for Xor")
    req(t === f, f, "Constant prop. failed for Xnor")
    req(!(!b), b, "Inverse rewrite failed for Not")
    req(b & false, false.to[Bit], "Right absorber failed for And")
    req(false & b, false.to[Bit], "Left absorber failed for And")
    req(b & true, b, "Right identity failed for And")
    req(true & b, b, "Left identity failed for And")
    req(b | true, true.to[Bit], "Right absorber failed for And")
    req(true | b, true.to[Bit], "Left absorber failed for And")
    req(b | false, b, "Right identity failed for And")
    req(false | b, b, "Left identity failed for And")
    req(b ^ false, b, "Right identity failed for Xor")
    req(false ^ b, b, "Left identity failed for Xor")

    val x = random[I32]
    val o = x.ones
    val y = 32.to[I32]
    val k = 16.to[I32]
    val g = 48.to[I32]
    val e = 512.to[I32]
    val z = 0.to[I32]
    req(-(-x), x, "Inverse rewrite failed for FixNeg")
    req(-y, -32.to[I32], "Constant prop. failed for FixNeg")
    req(~(~x), x, "Inverse rewrite failed for FixInv")
    req(~z, -1.to[I32], "Constant prop. failed for FixInv")
    req(x & 0, z, "Right absorber rewrite failed for FixAnd")
    req(0 & x, z, "Left absorber rewrite failed for FixAnd")
    req(o & x, x, "Left identity rewrite failed for FixAnd")
    req(x & o, x, "Right identity rewrite failed for FixAnd")
    req(y & k, z, "Constant prop. failed for FixAnd")

    req(x | 0, x, "Right identity rewrite failed for FixOr")
    req(0 | x, x, "Left identity rewrite failed for FixOr")
    req(x | o, o, "Right absorber rewrite failed for FixOr")
    req(o | x, o, "Left absorber rewrite failed for FixOr")
    req(y | k, g, "Constant prop. failed for FixOr")

    req(x + 0, x, "Right identity rewrite failed for FixAdd")
    req(0 + x, x, "Left identity rewrite failed for FixAdd")
    req(y + k, g, "Constant prop. failed for FixAdd")

    req(z - x, -x, "Rewrite to negation failed for FixSub")
    req(x - z, x, "Right identity rewrite failed for FixSub")
    req(y - k, k, "Constant prop. failed for FixSub")

    req(x * 0, z, "Right absorber rewrite failed for FixMul")
    req(0 * x, z, "Left absorber rewrite failed for FixMul")
    req(1 * x, x, "Left identity rewrite failed for FixMul")
    req(x * 1, x, "Right identity rewrite failed for FixMul")
    req(y * k, e, "Constant prop. failed for FixMul")

    req(0 / x, z, "Left absorber rewrite failed for FixDiv")
    req(x / 1, x, "Right identity rewrite failed for FixDiv")
    reqOp[FixInv[_,_,_]](1 / x, "Inverse rewrite failed for FixDiv")
    reqWarn(x / 0, "division by 0", "Warning on division by zero failed for FixDiv")
    req(y / k, 2.to[I32], "Constant prop. failed for FixDiv")



  }
}

object RewriteTests extends Testbench { val tests = Tests {
  'BasicRewriteChecks - test(BasicRewriteChecks)
}}

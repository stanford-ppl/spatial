package nova.test

import spatial.dsl._
import utest._

@spatial object BasicRewriteChecks extends Requirements {
  def main(): Void = {
    val b = random[Bit]
    val x = random[I32]
    val o = x.ones
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

    req(-(-x), x, "Inverse rewrite failed for FixNeg")
    req(~(~x), x, "Inverse rewrite failed for FixInv")
    req(x & 0, 0.to[I32], "Right absorber rewrite failed for FixAnd")
    req(0 & x, 0.to[I32], "Left absorber rewrite failed for FixAnd")
    req(o & x, x, "Left identity rewrite failed for FixAnd")
    req(x & o, x, "Right identity rewrite failed for FixAnd")
    req(x | 0, x, "Right identity rewrite failed for FixOr")
    req(0 | x, x, "Left identity rewrite failed for FixOr")
    req(x | o, o, "Right absorber rewrite failed for FixOr")
    req(o | x, o, "Left absorber rewrite failed for FixOr")
    req(x + 0, x, "Right identity rewrite failed for FixAdd")
    req(0 + x, x, "Left identity rewrite failed for FixAdd")
    req(0.to[I32] - x, -x, "Rewrite to negation failed for FixSub")
    req(x * 0, 0.to[I32], "Right absorber rewrite failed for FixMul")
    req(0 * x, 0.to[I32], "Left absorber rewrite failed for FixMul")
    req(1 * x, x, "Left identity rewrite failed for FixMul")
    req(x * 1, x, "Right identity rewrite failed for FixMul")
  }
}

object RewriteTests extends Testbench { val tests = Tests {
  'BasicRewriteChecks - test(BasicRewriteChecks)
}}

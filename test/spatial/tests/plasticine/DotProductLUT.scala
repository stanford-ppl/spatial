package spatial.tests.plasticine

import spatial.dsl._
import spatial.util.spatialConfig
import spatial.metadata.params._

@spatial class DotProductLUT extends SpatialTest {
  type X = FixPt[TRUE,_32,_0]

  val N = 32
  def dotproduct(aIn: Seq[scala.Int], bIn: Seq[scala.Int]): X = {
    val N = aIn.size
    val ip = 16
    val op = 1
    val ts  = N

    val B = ts
    val P1 = op
    val P2 = ip
    val P3 = ip

    val out = ArgOut[X]

    Accel {
      val aBlk = LUT[X](B)(aIn.map{ _.to[X] }:_*)
      val bBlk = LUT[X](B)(bIn.map{ _.to[X] }:_*)
      val accI = Reg[X](0.to[X])
      out := Reduce(accI)(B par P2){ii => aBlk(ii) * bBlk(ii) }{_+_}
    }
    getArg(out)
  }

  def main(args: Array[String]): Unit = {
    val a = Seq.tabulate(N){ i => i }
    val b = Seq.tabulate(N){ i => i }

    val result = dotproduct(a, b)
    val gold = a.zip(b).map { case (a,b) => a * b }.reduce { _ + _ }

    println("expected: " + gold)
    println("result: " + result)

    val cksum = gold == result
    println("PASS: " + cksum + " (DotProduct)")
    assert(cksum)
  }
}

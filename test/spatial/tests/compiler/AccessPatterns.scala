package spatial.tests.compiler

import argon._
import argon.schedule._
import argon.passes.IRPrinter
import spatial.dsl._
import spatial.flows._
import spatial.metadata.access._
import spatial.traversal.AccessAnalyzer

class AccessPatterns extends SpatialTestbench {
  lazy val scheduler = new SimpleScheduler(enableDCE = false)
  lazy val options = BlockOptions(Freq.Normal, Some(scheduler))
  var I: Int = _
  var J: Int = _
  var K: Int = _
  var A0: Void = _
  var A1: Void = _
  var A2: Void = _
  var A3: Void = _
  var A4: Void = _
  var Z: Int = _

  def program(): Block[_] = stageBlock({
    val a = SRAM[Int](32, 32)
    val x = SRAM[Int](32)
    val N = 32
    val M = 32

    Foreach(N by 1 par 2){i =>
      I = i
      val start = i%2
      Foreach(-start until M by 1){j =>
        J = j
        A0 = { a(i,j) = j }
      }
    }


    Foreach(0 until 32 by 2){k =>
      K = k
      val m = random[Int](32)
      Z = k / m
      A1 = { x(k / 2) = 3 }
      //A2 = { x(i / i) = 5 }
      A3 = { x(Z) = 2 }

      Foreach(-k until 32){j =>
        A4 = { x(j) = 4 }
      }

    }
  }, options)

  override def checks(): gen.Unit = {
    import poly.ISL

    implicit def expToProd(x: Int): Prod = Prod.single(x)
    implicit def intToProd(c: scala.Int): Prod = Prod.single(c)
    implicit def expToSum(x: Int): Sum = Sum.single(x)
    implicit def intToSum(c: scala.Int): Sum = Sum.single(c)

    lazy val accessAnalyzer = AccessAnalyzer(IR)
    lazy val printer = IRPrinter(IR, enable = true)

    SpatialFlowRules(IR) // Register standard flow analysis rules
    implicit val isl: ISL = new SpatialISL
    isl.startup()

    /*** Staging ***/
    val block = program()
    printer.run(block)
    accessAnalyzer.run(block)

    /*** Checks ***/

    val a00 = A0.affineMatrices(0)
    val a01 = A0.affineMatrices(1)
    val m00 = a00.matrix
    val m01 = a01.matrix
    System.out.println("m00: ")
    System.out.println(m00)
    System.out.println("m01: ")
    System.out.println(m01)
    System.out.println("m00 - m01 = 0 [overlap]:")
    val c0 = ((m00 - m01) === 0).andDomain
    System.out.println(c0)
    System.out.println(s"Overlaps: ${a00.overlapsAddress(a01)}")

    val a1 = A1.accessPatterns.head.head
    //val a2 = A2.accessPattern.head
    val a3 = A3.accessPatterns.head.head
    // TODO: Equality check is failing here for some reason, even when equal. Using string equality for now
    a1.comps.head.toString shouldBe AffineProduct(1, K).toString
    //a2.comps shouldBe Nil
    //a2.ofs shouldBe (1 : Sum)
    a3.comps shouldBe Nil
    a3.ofs.toString shouldBe Sum(Seq(Prod(Seq(Z),1)), 0).toString

    val a4 = A4.affineMatrices
    a4.foreach{matrix => System.out.println(matrix) }

    isl.shutdown()
  }

}

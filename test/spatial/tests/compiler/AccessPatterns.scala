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
  var A1: Void = _
  var A2: Void = _
  var A3: Void = _
  var Z: Int = _

  def program(): Block[_] = stageBlock({
    val x = SRAM[Int](32)

    Foreach(0 until 32 by 2){i =>
      System.out.println("Staging Foreach loop!")
      I = i
      val m = random[Int](32)
      Z = i / m
      A1 = { x(i / 2) = 3 }
      //A2 = { x(i / i) = 5 }
      A3 = { x(Z) = 2 }
    }
  }, options)

  override def checks(): gen.Unit = {
    implicit def expToProd(x: Int): Prod = Prod.single(x)
    implicit def intToProd(c: scala.Int): Prod = Prod.single(c)
    implicit def expToSum(x: Int): Sum = Sum.single(x)
    implicit def intToSum(c: scala.Int): Sum = Sum.single(c)

    lazy val accessAnalyzer = AccessAnalyzer(IR)
    lazy val printer = IRPrinter(IR, enable = true)
    SpatialFlowRules(IR) // Register standard flow analysis rules
    val block = program()

    printer.run(block)
    accessAnalyzer.run(block)

    val a1 = A1.accessPattern.head
    //val a2 = A2.accessPattern.head
    val a3 = A3.accessPattern.head
    // TODO: Equality check is failing here for some reason, even when equal
    a1.comps.head.toString shouldBe AffineProduct(1, I).toString
    //a2.comps shouldBe Nil
    //a2.ofs shouldBe (1 : Sum)
    a3.comps shouldBe Nil
    a3.ofs.toString shouldBe Sum(Seq(Prod(Seq(Z),1)), 0).toString
  }

}

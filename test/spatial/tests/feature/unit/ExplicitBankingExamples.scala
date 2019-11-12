package spatial.tests.feature.unit

import spatial.node._
import spatial.dsl._
import argon.Block
import argon.Op
import argon.node.FixMod

@spatial class ExplicitBankingExamples extends SpatialTest {
  override def runtimeArgs: Args = "4319728043"

  def main(args: Array[String]): Unit = {
    val numtests = 32
    val argins = List.tabulate(2){i => ArgIn[Int]}
    setArg(argins.head, 5)
    setArg(argins.last, 7)
    val argouts = List.tabulate(numtests){i => ArgOut[Int]}

    // Create HW accelerator
    Accel {
      List.tabulate(numtests){i =>
        val sram = if (i < 16) SRAM[Int](100, 200).bank(N = List(i/3, i/4), B = List(1,1), alpha = List(i%4, i%5))
                   else SRAM[Int](200,100).bank(N = List(i), B = List(1), alpha = List(i%3,i%4))
        argouts(i) = sram(argins.head,argins.last)
      }
//      argouts.zipWithIndex.foreach{case (a, i) => a := N.value % mods(i)}
    }

    argouts.zipWithIndex.foreach{case (a, i) =>
        println(r"${getArg(a)}")
    }
  }
//  override def checkIR(block: Block[_]): Result = {
//    val modcount = block.nestedStms.collect{case x@Op(FixMod(_,_)) => x }.size
//
//    require(modcount == 0, "All mods should have been rewritten?")
//
//    super.checkIR(block)
//  }

}

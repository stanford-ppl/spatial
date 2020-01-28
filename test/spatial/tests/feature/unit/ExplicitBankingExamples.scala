package spatial.tests.feature.unit

import spatial.node._
import spatial.dsl._
import argon.Block
import argon.Op
import argon.node.FixMod

@spatial class ExplicitBankingExamples extends SpatialTest {
  override def backends = Seq(Zynq, ZCU)
  override def runtimeArgs: Args = "4319728043"

  def main(args: Array[String]): Unit = {
    val numtests = 16
    val argins = List.tabulate(2){i => ArgIn[Int]}
    setArg(argins.head, 5)
    setArg(argins.last, 7)
    val argouts = List.tabulate(numtests){i => ArgOut[Int]}

    // Create HW accelerator
    Accel {
      List.tabulate(numtests){i =>
        val sram = if (i < numtests/2) SRAM[Int](100, 200).forcebank(N = List(i/3 + 1, i/4 + 1), B = List(1,1), alpha = List(i%9+1, i%13))
                   else SRAM[Int](200,100).forcebank(N = List(i + 1), B = List(1), alpha = List(i%7+1,i%10))
        sram(argins.last,argins.head) = 1
        sram(argins.head,argins.head) = 1
        sram(argins.last,argins.last) = 1
        sram(argins.head,argins.last) = 1

        val r = List.tabulate(5){i =>
          val x = Reg[Int]
          Pipe {
            x := sram(i, argins.last)
          }
          x
        }
        argouts(i) := r.map(_.value).reduceTree{_+_}
      }
//      argouts.zipWithIndex.foreach{case (a, i) => a := N.value % mods(i)}
    }

    argouts.zipWithIndex.foreach{case (a, i) =>
        println(r"${getArg(a)}")
        assert(getArg(argins.head) == 5)
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

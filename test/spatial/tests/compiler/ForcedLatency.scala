package spatial.tests.compiler

import spatial.dsl._
import argon.{Block, Op}
import spatial.node.RegWrite
import spatial.metadata.memory._
import spatial.metadata.retiming._

@spatial class ForcedLatency extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val y = ArgIn[Int]
    val z = ArgOut[Int]
    setArg(y, 3)
    Accel {
      Sequential {
        val a = Reg[Int](0)
        a.explicitName = "TestReg"
        spatial.dsl.ForcedLatency(0.0) {
          a := y + 2
        }
        z := a
      }
    }
    println("z: " + getArg(z))
    assert(getArg(z) == 5.to[Int])
  }

  override def checkIR(block: Block[_]): Result = {
    block.nestedStms.foreach {
      case Op(wr: RegWrite[_]) if wr.mem.explicitName.getOrElse("").eq("TestReg") =>
        require(wr.data.hasForcedLatency)
        require(wr.data.fullDelay == 0.0)
      case _ => None
    }

    super.checkIR(block)
  }
}
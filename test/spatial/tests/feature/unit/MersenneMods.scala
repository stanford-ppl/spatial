package spatial.tests.feature.unit

import spatial.node._
import spatial.dsl._
import argon.Block
import argon.Op
import argon.node.{FixMod,FixDiv}
import spatial.metadata.control._

@spatial class MersenneMods extends SpatialTest {
  override def runtimeArgs: Args = "4319728043"

  def main(args: Array[String]): Unit = {
    val mods = List.tabulate(14){i => scala.math.pow(2,i + 2)-1}
    val argouts = List.tabulate(14){i => ArgOut[Int]}
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    // Create HW accelerator
    Accel {
      argouts.zipWithIndex.foreach{case (a, i) => a := N.value % mods(i)}
    }

    argouts.zipWithIndex.foreach{case (a, i) =>
        println(r"${getArg(a) == args(0).to[Int] % mods(i)} for x % ${mods(i)} got ${getArg(a)}, wanted ${args(0).to[Int] % mods(i)}")
        assert(getArg(a) == args(0).to[Int] % mods(i))
    }
  }
  override def checkIR(block: Block[_]): Result = {
    val modcount = block.nestedStms.collect{case x@Op(_:FixMod[_,_,_]) if x.parent.isAccel => x }.size

    require(modcount == 0, "All mods should have been rewritten?")

    super.checkIR(block)
  }

}

@spatial class SecondOrderMersenneMods extends SpatialTest {
  override def runtimeArgs: Args = "4319728043"

  def main(args: Array[String]): Unit = {
    val mods = List(5, 9, 17, 21)
    val argins = List.tabulate(4){i => ArgIn[Int]}
    argins.zip(mods).foreach{case (a,b) => setArg(a, b)}
    val argouts = List.tabulate(4){i => ArgOut[Int]}
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    // Create HW accelerator
    Accel {
      argouts.zipWithIndex.foreach{case (a, i) => a := N.value % mods(i)}
    }

    argouts.zipWithIndex.foreach{case (a, i) =>
        println(r"${getArg(a) == args(0).to[Int] % mods(i)} for x % ${mods(i)} got ${getArg(a)}, wanted ${args(0).to[Int] % mods(i)}")
        assert(getArg(a) == args(0).to[Int] % mods(i))
    }
  }
  override def checkIR(block: Block[_]): Result = {
    val modcount = block.nestedStms.collect{case x@Op(_:FixMod[_,_,_]) if x.parent.isAccel => x }.size

    require(modcount == 0, "All mods should have been rewritten?")

    super.checkIR(block)
  }
}

@spatial class MersenneDivs extends SpatialTest {
  override def runtimeArgs: Args = "4319728043"

  def main(args: Array[String]): Unit = {
    val max_exp = 3
    val divs = List.tabulate(max_exp){i => scala.math.pow(2,i + 2)-1}
    val argouts = List.tabulate(max_exp){i => ArgOut[Int]}
    val N = ArgIn[Int]
    setArg(N, args(0).to[Int])

    // Create HW accelerator
    Accel {
      argouts.zipWithIndex.foreach{case (a, i) => a := N.value / divs(i)}
    }

    argouts.zipWithIndex.foreach{case (a, i) =>
        println(r"${getArg(a) == args(0).to[Int] / divs(i)} for x / ${divs(i)} got ${getArg(a)}, wanted ${args(0).to[Int] / divs(i)}")
        assert(getArg(a) == args(0).to[Int] / divs(i))
    }
  }
  override def checkIR(block: Block[_]): Result = {
    val modcount = block.nestedStms.collect{case x@Op(_:FixDiv[_,_,_]) if x.parent.isAccel => x }.size

    require(modcount == 0, "All mods should have been rewritten?")

    super.checkIR(block)
  }

}


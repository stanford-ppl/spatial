// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import org.scalatest.Assertions._

class MetapipeTests(c: Metapipe) extends PeekPokeTester(c) {
  val numIters = List(0,1,3,4,5,6,7,8)
  val latencies = (0 until c.n).map { i => 
    poke(c.io.input.stageMask(i), 1)
    math.abs(rnd.nextInt(10)) + 2 
  } 
  var stageCounts = Array.tabulate(c.n) { i => 0 }
  var stageDones = Array.tabulate(c.n) { i => 0 }
  latencies.map { a => println("latency of stage = " + a)}
  val timeout = 500
  numIters.foreach{ numIter => 
    step(50)
    poke(c.io.input.numIter, numIter)
    poke(c.io.input.enable, 1)

    def executeStage(s: Int) {
      val numCycles = latencies(s)
      stageCounts(s) += 1
      if (stageCounts(s) > latencies(s)) {
        poke(c.io.input.stageDone(s), 1)
        stageCounts(s) = 0
        stageDones(s) += 1
      } else {
        poke(c.io.input.stageDone(s), 0)
      }
    }

    def handleStageEnables = {
      (0 until c.n).foreach { i => 
        val stageEn = peek(c.io.output.stageEnable(i)).toInt
        if (stageEn == 1) {
          executeStage(i)
        }
      }
    }

    // Start

    var done = peek(c.io.output.done).toInt
    var numCycles = 0
    while ((done != 1) & (numCycles < timeout)) {
      handleStageEnables
      done = peek(c.io.output.done).toInt
      step(1)
      (0 until c.n).foreach { i => poke(c.io.input.stageDone(i), 0) }
      numCycles += 1
    }
    if ( (numCycles > timeout) | (numCycles < 2) ) {
      expect(c.io.output.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
    }
    expect(c.io.output.done, 0)
    poke(c.io.input.enable, 0)
    (0 until c.n).foreach { i => 
      if (stageDones(i) != numIter) {
        println(s"expect stage $i to have $numIter, but it has ${stageDones(i)}")
        expect(c.io.output.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
      }
      stageDones(i) = 0
    }

  }


}

class MetapipeTester extends ChiselFlatSpec {
  behavior of "Metapipe"
  backends foreach {backend =>
    it should s"correctly add randomly generated numbers $backend" in {
      Driver(() => new Metapipe(5))(c => new MetapipeTests(c)) should be (true)
    }
  }
}




// class MetapipeTests(c: Metapipe) extends PlasticineTester(c) {
//   val numIter = 5
//   val stageIterCount = List.tabulate(c.numInputs) { i => math.abs(rnd.nextInt) % 10 }
//   println(s"stageIterCount: $stageIterCount")

//   def executeStages(s: List[Int]) {
//     val numCycles = s.map { stageIterCount(_) }
//     var elapsed = 0
//     var done: Int = 0
//     while (done != s.size) {
//       c.io.stageDone.foreach { poke(_, 0) }
//       step(1)
//       elapsed += 1
//       for (i <- 0 until s.size) {
//         if (numCycles(i) == elapsed) {
//           println(s"[Stage ${s(i)} Finished execution at $elapsed")
//           poke(c.io.stageDone(s(i)), 1)
//           done += 1
//         }
//       }
//     }
//     c.io.stageDone.foreach { poke(_, 1) }
//   }

//   def handleStageEnables = {
//     val stageEnables = c.io.stageEnable.map { peek(_).toInt }
//     val activeStage = stageEnables.zipWithIndex.filter { _._1 == 1 }.map { _._2 }
// //    executeStage(activeStage)
//   }

//   // Start
//   poke(c.io.numIter, numIter)
//   poke(c.io.enable, 1)

//   var done = peek(c.io.done).toInt
//   var numCycles = 0
//   while ((done != 1) & (numCycles < 100)) {
//     handleStageEnables
//     done = peek(c.io.done).toInt
//     step(1)
//     numCycles += 1
//   }
// }


// object MetapipeTest {

//   def main(args: Array[String]): Unit = {
//     val (appArgs, chiselArgs) = args.splitAt(args.indexOf("end"))

//     val numInputs = 2
//     chiselMainTest(chiselArgs, () => Module(new Metapipe(numInputs))) {
//       c => new MetapipeTests(c)
//     }
//   }
// }
// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import org.scalatest.Assertions._

class OuterControllerTests(c: OuterController) extends PeekPokeTester(c) {
  val numIters = List(1,2,3,4,5,6,7,8)
  val latencies = (0 until c.depth).map { i => 
    poke(c.io.maskIn(i), 1)
    math.abs(rnd.nextInt(10)) + 2 
  } 

  var counter = 0

  var stageCounts = Array.fill(c.depth)(0)
  var stageDones = Array.fill(c.depth)(0)
  latencies.map { a => println("latency of stage = " + a)}
  val timeout = 1000
  numIters.foreach{ numIter => 
    counter = 0
    poke(c.io.rst, 1)
    step(1)
    poke(c.io.rst, 0)
    step(50)
    poke(c.io.enable, 1)

    def executeStage(s: Int) {
      val numCycles = latencies(s)
      stageCounts(s) += 1
      if (stageCounts(s) > latencies(s)) {
        poke(c.io.doneIn(s), 1)
        stageCounts(s) = 0
        stageDones(s) += 1
      } else {
        poke(c.io.doneIn(s), 0)
      }
    }

    def handleStageEnables = {
      (0 until c.depth).foreach { i => 
        val stageEn = peek(c.io.enableOut(i)).toInt
        if (stageEn == 1) {
          executeStage(i)
        }
      }
    }

    // Start

    var done = peek(c.io.done).toInt
    var numCycles = 0
    while ((done != 1) & (numCycles < timeout)) {
      handleStageEnables
      done = peek(c.io.done).toInt
      counter = if (peek(c.io.ctrInc) == 1) counter + 1 else counter
      if (counter == numIter) poke(c.io.ctrDone, 1)
      step(1)
      poke(c.io.ctrDone, 0)
      (0 until c.depth).foreach { i => poke(c.io.doneIn(i), 0) }
      numCycles += 1
    }
    if ( (numCycles > timeout) | (numCycles < 2) ) {
      expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
    }
    expect(c.io.done, 0)
    poke(c.io.enable, 0)
    (0 until c.depth).foreach { i => 
      if (stageDones(i) != numIter) {
        println(s"expect stage $i to have $numIter, but it has ${stageDones(i)}")
        expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
      }
      stageDones(i) = 0
    }

  }


}



class InnerControllerTests(c: InnerController) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  val timeout = 999

  val maxes = (0 until 2).map { i => math.abs(rnd.nextInt(20)) + 2 } 
  maxes.map { a => println("max of ctr = " + a) }
  var cnts = Array.tabulate(2) { i => 0 }

  def handleStep {
    val cnt_en = peek(c.io.ctrInc).toInt
    if (cnt_en == 1) {
      cnts(0) += 1
      // println(s"cnts ${cnts(0)} ${cnts(1)}")
      (0 until 1).foreach { i =>
        val m = maxes(i)
        if (cnts(i) >= m) {
          cnts(i+1) += 1
          cnts(i) = 0
        }
      }
      val last = maxes.last
      if (cnts.last == last) {
        poke(c.io.ctrDone, 1)
      }
    }
    step(1)
    poke(c.io.ctrDone,0)
  }

  poke(c.io.rst, 1)
  step(1)
  poke(c.io.rst, 0)
  step(1)
  poke(c.io.enable, 1)

  var numEnCycles = 0
  var done = peek(c.io.done).toInt
  while (done != 1) {
    handleStep
    done = peek(c.io.done).toInt
    val cnt_en = peek(c.io.ctrInc).toInt
    if (cnt_en == 1) numEnCycles += 1
  }
  poke(c.io.enable, 0)
  poke(c.io.rst, 1)
  step(1)
  poke(c.io.rst, 0)
  if ( (numEnCycles > timeout) | (numEnCycles < 2) ) {
    println("ERROR: Either timeout or did not run at all! (" + numEnCycles + " cycles)")
    expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  val expectedCycs = maxes.reduce{_*_}
  if ( numEnCycles != expectedCycs ) {
    println(s"ERROR: Ran ${numEnCycles} but expected ${expectedCycs} cycs!")
    expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  println(s"numiters $numEnCycles")
  step(20)

  poke(c.io.rst, 1)
  step(1)
  poke(c.io.rst, 0)
  step(1)

  // poke(c.io.enable, 1)
  // step(1)
  // numEnCycles = 0
  // done = peek(c.io.done).toInt
  // (0 until 2).foreach { i => cnts(i) = 0 }
  // while (done != 1) {
  //   println(s"stepping. numEncycls = $numEnCycles")
  //   handleStep
  //   done = peek(c.io.done).toInt
  //   val cnt_en = peek(c.io.ctrInc).toInt
  //   if (cnt_en == 1) numEnCycles += 1
  // }
  // poke(c.io.enable, 0)
  // step(5)

  // if ( (numEnCycles > timeout) | (numEnCycles < 2) ) {
  //   println("ERROR: Either timeout or did not run at all! (" + numEnCycles + " cycles)")
  //   expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  // }
  // if ( numEnCycles != expectedCycs ) {
  //   println(s"ERROR: Ran ${numEnCycles} but expected ${expectedCycs} cycs!")
  //   expect(c.io.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  // }
  // println(s"numiters $numEnCycles")

}

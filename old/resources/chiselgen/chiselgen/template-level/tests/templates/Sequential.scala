// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import org.scalatest.Assertions._

class SeqpipeTests(c: Seqpipe) extends PeekPokeTester(c) {
  val numIters = List(0,4,5,8,12)
  val latencies = (0 until c.n).map { i => 
    poke(c.io.input.stageMask(i), 1)
    math.abs(rnd.nextInt(10)) + 2 
  } 
  latencies.map { a => println("latency of stage = " + a)}
  val timeout = 500
  var stageDones = Array.tabulate(c.n) { i => 0 }

  numIters.foreach { numIter => 
    def executeStage(s: Int) {
      val numCycles = latencies(s)
      // println(s"[stage $s] Executing for $numCycles")
      step(numCycles)
      // println(s"[stage $s] Done")
      stageDones(s) += 1
      poke(c.io.input.stageDone(s), 1)
      step(1)
      poke(c.io.input.stageDone(s), 0)
    }

    def handleStageEnables = {
      val stageEnables = c.io.output.stageEnable.map { peek(_).toInt }
      val activeStage = stageEnables.indexOf(1)
      // println(s"active stage $activeStage")
      if (activeStage != -1) executeStage(activeStage)
    }

    // Start
    poke(c.io.input.numIter, numIter)
    poke(c.io.input.enable, 1)

    var done = peek(c.io.output.done).toInt
    var numCycles = 0
    while ((done != 1) & (numCycles < timeout)) {
      handleStageEnables
      done = peek(c.io.output.done).toInt
      step(1)
      numCycles += 1
    }
    if ( (numCycles > timeout) | (numCycles < 2) ) {
      println("ERROR: Either timeout or did not run at all!")
      expect(c.io.output.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
    }
    expect(c.io.output.done, 0)
    (0 until c.n).foreach { i => 
      if (stageDones(i) != numIter) {
        println(s"expect stage $i to have $numIter, but it has ${stageDones(i)}")
        expect(c.io.output.done, 999) // TODO: Figure out how to "expect" signals that are not hw IO
      }
      stageDones(i) = 0
    }

    poke(c.io.input.enable, 0)

    step(50)


  }
}

class SeqpipeTester extends ChiselFlatSpec {
  behavior of "Seqpipe"
  backends foreach {backend =>
    it should s"correctly add randomly generated numbers $backend" in {
      Driver(() => new Seqpipe(10))(c => new SeqpipeTests(c)) should be (true)
    }
  }
}

// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}

class ParallelTests(c: Parallel) extends PeekPokeTester(c) {
  val latencies = (0 until c.n).map { i => 
    poke(c.io.input.stageMask(i), 1)
    math.abs(rnd.nextInt(8)) + 5 
  } 
  latencies.map { a => println("latency of stage = " + a)}
  val maxCycles = latencies.reduce{(a,b) => if (a > b) a else b} + 20
  step(1)
  reset(1)
  poke(c.io.input.enable, 1)
  for (t <- 0 until maxCycles) {
    val stagesDone = latencies.map { _ > t }
    (0 until c.n).foreach {i => poke(c.io.input.stageDone(i), t - latencies(i) == 1) }

    step(1)
    val stagesEnabled = (0 until c.n).map {i => peek(c.io.output.stageEnable(i)) }

    (0 until c.n).foreach {i => 
      // val a = peek(c.io.output.stageEnable(i))
      // println("stage " + i + " is " + a + ", wanted " + {t>=0 & t <=latencies(i)})
      expect(c.io.output.stageEnable(i), t >= 0 & t <= latencies(i))
    }

    val isDone = peek(c.io.output.done)
    if (isDone == 1) poke(c.io.input.enable, 0)

  }

  // Do it again
  poke(c.io.input.enable, 1)
  for (t <- 0 until maxCycles) {
    val stagesDone = latencies.map { _ > t }
    (0 until c.n).foreach {i => poke(c.io.input.stageDone(i), t - latencies(i) == 1) }

    step(1)
    val stagesEnabled = (0 until c.n).map {i => peek(c.io.output.stageEnable(i)) }

    (0 until c.n).foreach {i => 
      // val a = peek(c.io.output.stageEnable(i))
      // println("stage " + i + " is " + a + ", wanted " + {t>=0 & t <=latencies(i)})
      expect(c.io.output.stageEnable(i), t >= 0 & t <= latencies(i))
    }

    val isDone = peek(c.io.output.done)
    if (isDone == 1) poke(c.io.input.enable, 0)

  }
}

class ParallelTester extends ChiselFlatSpec {
  behavior of "Parallel"
  backends foreach {backend =>
    it should s"correctly add randomly generated numbers $backend" in {
      Driver(() => new Parallel(3))(c => new ParallelTests(c)) should be (true)
    }
  }
}

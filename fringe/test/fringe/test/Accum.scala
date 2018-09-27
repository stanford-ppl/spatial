// See LICENSE.txt for license details.
package fringe.test

import fringe.templates.memory.FixFMAAccum
import fringe.utils.log2Up

import chisel3.iotesters.PeekPokeTester


/** FF test harness */
class FixFMAAccumTests(c: FixFMAAccum) extends PeekPokeTester(c) {
  val numResets = 3
  var count = 0

  for (iter <- 0 until numResets) {
    poke(c.io.reset(0), 1)
    step(1)
    poke(c.io.reset(0), 0)
    step(1)

    count = 0
    for(i <- 0 until 10) {
      val next = rnd.nextInt(10)
      count = next * next + count
      poke(c.io.input1(0), next)
      poke(c.io.input2(0), next)
      poke(c.io.enable(0), 1)
      step(1)
    }
    poke(c.io.enable(0), 0)
    step(c.fmaLatency.toInt + log2Up(c.cycleLatency.toInt)) // Should be sufficiently long for drain
    val r = peek(c.io.output)
    Console.println(s"got $r, expected $count")
    expect(c.io.output, count)
  }
}

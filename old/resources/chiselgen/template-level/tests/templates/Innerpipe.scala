// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import org.scalatest.Assertions._

class InnerpipeTests(c: Innerpipe) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  poke(c.io.input.enable, 1)
  poke(c.io.input.forever, 0)
  val timeout = 999

  val maxes = (0 until 2).map { i => math.abs(rnd.nextInt(20)) + 2 } 
  maxes.map { a => println("max of ctr = " + a) }
  var cnts = Array.tabulate(2) { i => 0 }

  // (0 until 2).foreach { i => poke(c.io.input.ctr_maxIn(i), maxes(i))}

  def handleStep {
    val cnt_en = peek(c.io.output.ctr_inc).toInt
    if (cnt_en == 1) {
      cnts(0) += 1
      // println(s"cnts ${cnts(0)} ${cnts(1)}")
      (0 until 2-1).foreach { i =>
        val m = maxes(i)
        if (cnts(i) >= m) {
          cnts(i+1) += 1
          cnts(i) = 0
        }
      }
      val last = maxes(2-1)
      if (cnts(2-1) == last) {
        poke(c.io.input.ctr_done, 1)
      }
    }
    step(1)
    poke(c.io.input.ctr_done,0)
  }

  poke(c.io.input.rst, 1)
  step(1)
  poke(c.io.input.rst, 0)

  var numEnCycles = 0
  var done = peek(c.io.output.extendedDone).toInt
  while (done != 1) {
    handleStep
    done = peek(c.io.output.extendedDone).toInt
    val cnt_en = peek(c.io.output.ctr_inc).toInt
    if (cnt_en == 1) numEnCycles += 1
  }
  poke(c.io.input.enable, 0)
  poke(c.io.input.rst, 1)
  step(1)
  poke(c.io.input.rst, 0)
  if ( (numEnCycles > timeout) | (numEnCycles < 2) ) {
    println("ERROR: Either timeout or did not run at all! (" + numEnCycles + " cycles)")
    expect(c.io.output.extendedDone, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  val expectedCycs = maxes.reduce{_*_} - 1
  if ( numEnCycles != expectedCycs ) {
    println(s"ERROR: Ran ${numEnCycles} but expected ${expectedCycs} cycs!")
    expect(c.io.output.extendedDone, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  println(s"numiters $numEnCycles")
  step(20)

  poke(c.io.input.rst, 1)
  step(1)
  poke(c.io.input.rst, 0)
  step(1)

  poke(c.io.input.enable, 1)
  step(1)
  numEnCycles = 0
  done = peek(c.io.output.extendedDone).toInt
  (0 until 2).foreach { i => cnts(i) = 0 }
  while (done != 1) {
    handleStep
    done = peek(c.io.output.extendedDone).toInt
    val cnt_en = peek(c.io.output.ctr_inc).toInt
    if (cnt_en == 1) numEnCycles += 1
  }
  poke(c.io.input.enable, 0)

  if ( (numEnCycles > timeout) | (numEnCycles < 2) ) {
    println("ERROR: Either timeout or did not run at all! (" + numEnCycles + " cycles)")
    expect(c.io.output.extendedDone, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  if ( numEnCycles != expectedCycs ) {
    println(s"ERROR: Ran ${numEnCycles} but expected ${expectedCycs} cycs!")
    expect(c.io.output.extendedDone, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }
  println(s"numiters $numEnCycles")

}

class InnerpipeTester extends ChiselFlatSpec {
  behavior of "Innerpipe"
  backends foreach {backend =>
    it should s"correctly add randomly generated numbers $backend" in {
      Driver(() => new Innerpipe(false))(c => new InnerpipeTests(c)) should be (true)
    }
  }
}

// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}


class FILOTests(c: FILO) extends PeekPokeTester(c) {
  reset(1)
  step(5)
  var element = 0
  val p = scala.math.max(c.pW, c.pR)

  def push(inc: Boolean = true) {
    (0 until c.pW).foreach { i => poke(c.io.in(i), element + i) }
    if (inc) element += c.pW
    poke(c.io.push(0), 1)
    step(1)
    poke(c.io.push(0),0)
  }
  def pop(inc: Boolean = true) {
    if (inc) {
      (0 until c.pR).foreach { i => 
        val expected = element - i -1
        val index = c.pR-1-i

        // val a = peek(c.io.out(index))
        // println(s"Expect $expected, got $a (error ${a != expected})")

        expect(c.io.out(index), expected) 
      }
      element -= c.pR
    }
    poke(c.io.pop(0), 1)
    step(1)
    poke(c.io.pop(0),0)
  }

  // fill FILO halfway
  for (i <- 0 until c.depth/c.pW/2) {
    push()
  }

  // hold for a bit
  step(5)

  // pop FILO halfway
  for (i <- 0 until c.depth/c.pR/2) {
    pop()
  }

  // pop to overread
  expect(c.io.debug.overread, 0)
  (0 until (p/c.pR)).foreach{ i => pop(false) }
  expect(c.io.debug.overread, 1)
  (0 until (p/c.pW)).foreach{ i => push(false) }
  expect(c.io.debug.overread, 0)
  (0 until c.depth/c.pW).foreach { i => push(false) }
  expect(c.io.debug.overwrite, 0)
  (0 until (p/c.pW)).foreach{ i => push(false) }
  expect(c.io.debug.overwrite, 1)
  (0 until (p/c.pR)).foreach{ i => pop(false) }
  expect(c.io.debug.overwrite, 0)
  (0 until c.depth/c.pR).foreach { i => pop(false) }



  // randomly push 'n pop
  val numTransactions = c.depth*10
  for (i <- 0 until numTransactions) {
    val newenable = rnd.nextInt(4)
    if (element < 2*p) for (k <- 0 until p / c.pW) push()
    else if (element >= (c.depth/p)-p) for (k <- 0 until p / c.pR) pop()
    else if (newenable == 1) for (k <- 0 until p / c.pW) {push()}
    else if (newenable == 2) for (k <- 0 until p / c.pR) {pop()}
    else step(1)
  }
  
}

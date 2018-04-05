// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import org.scalatest.Assertions._

class PRNGTests(c: PRNG) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  val numShifts = 10000
  var numbers = List[BigInt]()
  for (i <- 0 until numShifts) {
    // if (i % 3 == 0) { // For 3 cycle rng
    //   val num = peek(c.io.output)
    //   numbers = numbers :+ num      
    // }
    
    val num = peek(c.io.output)
    numbers = numbers :+ num      

    poke(c.io.en, 1)
    step(1)
  }

  // println(numbers.mkString(" "))
  val mean = numbers.reduce{_+_} / numbers.length
  println("Mean:          " + mean)
  println("2^32 Midpoint: 2147483647.5")

  // Dummy error if check fails
  if ( scala.math.abs((mean - 2147483647).toDouble) > 100000000 ) {
    expect(c.io.output, 0) // throw error
  }
}


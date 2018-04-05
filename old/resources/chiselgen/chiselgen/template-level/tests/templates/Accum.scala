// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}


/**
 * FF test harness
 */
class UIntAccumTests(c: UIntAccum) extends PeekPokeTester(c) {
  val numResets = 3
  var init = 0
  var count = 0
  poke(c.io.init, init)
  step(1)

  for (iter <- 0 until numResets) {
    count = 0
    for(i <- 0 until 50) {
      val acc = rnd.nextInt(10)
      val en = rnd.nextInt(2)
      val next = c.lambda match {
        case "add" => count + acc  
        case "max" => if(count > acc) count else acc  
        case "min" => if(count < acc) count else acc
      }
      count = if(en == 1) next else count
      poke(c.io.next, acc)
      poke(c.io.enable, en)
      step(1)
      // expect(c.io.output, count)
    }
    poke(c.io.enable, 0)
    step(3)
    poke(c.io.reset, 1)
    step(1)
    poke(c.io.reset, 0)
    step(1)

    count = 0
  }
  step(5)
}

class SpecialAccumTests(c: SpecialAccum) extends PeekPokeTester(c) {
  val numResets = 3
  var init = 0
  var result: Int = 0
  var count: Double = 0
  poke(c.io.input.init, init)
  step(1)

  for (iter <- 0 until numResets) {
    count = 0
    for(i <- 0 until 50) {
      val acc = c.typ match {
        case "UInt" => 
          val num = rnd.nextInt(10)
          poke(c.io.input.next, num)
          num.toDouble
        case "FixedPoint" =>
          val num = rnd.nextFloat*10
          val acc_rounded = ((num*scala.math.pow(2,c.params(2))).toInt)/scala.math.pow(2,c.params(2))
          poke(c.io.input.next, (num*scala.math.pow(2,c.params(2))).toInt)
          acc_rounded
        case "FloatingPoint" => 
          val num = rnd.nextInt(10)
          poke(c.io.input.next, num)
          num.toDouble
      }
      val en = rnd.nextInt(2)
      val next = c.lambda match {
        case "add" => (count + acc).toDouble
        case "max" => if(count > acc) count else acc  
        case "min" => if(count < acc) count else acc
      }
      step(1)
      poke(c.io.input.enable, en)
      val (hw, factor) = c.typ match {
        case "UInt" => (peek(c.io.output), 1)
        case "FixedPoint" => (peek(c.io.output), scala.math.pow(2,c.params(2)).toInt)
        case "FloatingPoint" => (peek(c.io.output), 1)
      }
      // println(s"next: $acc into $count = $next (en $en), got ${hw.toDouble/scala.math.pow(2,c.params(2))}")
      count = if(en == 1) next else count
      result = (count*factor).toInt
    }
    poke(c.io.input.enable, 0)
    step(c.latency)
    // expect(c.io.output, result)
    step(3)
    poke(c.io.input.reset, 1)
    step(1)
    poke(c.io.input.reset, 0)
    step(1)
  }


  // step(5)
}

// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}


/**
 * SingleCounter test harness
 */

class SingleCounterTests(c: SingleCounter) extends PeekPokeTester(c) {

  // def check(wire: Any, value: Any, printon: Bool) {
  //   val a = peek(wire)
  //   if (printon) println("Expect " + a + " to be " + value)
  //   expect(wire, value)
  // }

  var numEnabledCycles = 0
  var expectedCount = 0
  var expectedDone = 0

  val stops = List(64)
  val strides = List(1, 6, 7)
  val starts = List(0, 5)

  step(1)
  reset(1)

  stops.foreach { stop => 
    strides.foreach { stride => 
      starts.foreach { start =>
        numEnabledCycles = 0
        var saturate = 1
        val gap = 0
        var enable = 1

        def testOneStep() = {
          step(1)
          numEnabledCycles += enable
          val count = saturate match {
            case 1 =>
              val count = if (start + numEnabledCycles * (gap + stride*c.par) < stop) {
                (start + numEnabledCycles * (gap + stride*(c.par))) 
              } else {
                if ((stop-start) % (gap + stride*(c.par)) == 0) (stop - (gap + stride*(c.par))) else stop - (stop-start) % (gap + stride*(c.par))
              }

              count
            case 0 =>
              val numSteps = ( (stop-start) / (gap + stride*c.par)) // integer type
              val numUntilWrap = if (numSteps * (gap + stride*c.par) == (stop-start)) numSteps else numSteps+1
              val numWrappedEnabledCycles = numEnabledCycles % numUntilWrap
              val count = if (start + numWrappedEnabledCycles * (gap + stride*c.par) < stop) (start + numWrappedEnabledCycles * (gap + stride*(c.par))) else (stop - stop % (gap + stride*(c.par)))

              count
          }
          val done = if ( (count + c.par*stride + gap >= stop) & (enable == 1) ) 1 else 0
          // val a = peek(c.io.output.count(0))
          // val b = peek(c.io.output.count(1))
          // val d = peek(c.io.output.count(2))
          // val cc = peek(c.io.output.done)
          // println(s"SingleCounters at $a, $b, (want $count), stop $stop done? $cc expected? $done because ${(count + c.par*stride + gap)} satmode $saturate")
          // if (cc != done | a != count | b != {count + stride} | d != {count + 2*stride}) println("           ERROR!!!!!!!!!!!!!! \n\n")

          // Check signal values
          (0 until c.par).foreach { i => expect(c.io.output.count(i), count + (i * stride)) }
          expect(c.io.output.done, done)


          expectedCount = count
          expectedDone = done
        }

        poke(c.io.input.enable, 0)
        poke(c.io.input.start, start)
        step(5)
        poke(c.io.input.stride, stride)
        poke(c.io.input.reset, 1)
        step(1)
        poke(c.io.input.stop, stop)
        poke(c.io.input.gap, gap)
        poke(c.io.input.enable, enable)
        poke(c.io.input.saturate, saturate)
        poke(c.io.input.reset, 0)


        for (i <- 1 until 5) {
          testOneStep()
        }

        // Test stall
        enable = 0
        poke(c.io.input.enable, enable)
        for (i <- 1 until 5) {
          testOneStep()
        }

        // Continue
        enable = 1
        poke(c.io.input.enable, enable)
        for (i <- 1 until stop) {
          testOneStep()
        }

        // Reset and go again
        numEnabledCycles = 0
        poke(c.io.input.reset, 1)
        step(1)
        poke(c.io.input.reset, 0)
        for (i <- 1 until stop+2) {
          testOneStep()
        }

        // Reset and test non-saturating mode
        saturate = 0
        poke(c.io.input.saturate, saturate)
        numEnabledCycles = 0
        poke(c.io.input.reset, 1)
        step(1)
        poke(c.io.input.reset, 0)
        for (i <- 1 until stop+2) {
          testOneStep()
        }

        poke(c.io.input.enable, 0)
        poke(c.io.input.reset, 1)
        step(1)
        reset(1)
        poke(c.io.input.reset, 0)

      }
    }
  }

}

class CompactingCounterTests(c: CompactingCounter) extends PeekPokeTester(c) {

  // def check(wire: Any, value: Any, printon: Bool) {
  //   val a = peek(wire)
  //   if (printon) println("Expect " + a + " to be " + value)
  //   expect(wire, value)
  // }

  var numEnabledCycles = 0
  var expectedCount = 0
  var expectedDone = 0

  step(1)
  reset(1)

  var enable = 0

  def testOneStep(ens: Seq[Int]) = {
    step(1)
    val num_enabled = ens.reduce{_+_}
    numEnabledCycles += num_enabled
    (0 until c.lanes).foreach{i => poke(c.io.input.enables(i), ens(i))}
    step(1)
    (0 until c.lanes).foreach{i => poke(c.io.input.enables(i), 0)}

    val done = if ( ((numEnabledCycles % c.depth) + num_enabled >= c.depth) & (enable == 1) ) 1 else 0
    // val a = peek(c.io.output.count(0))
    // val b = peek(c.io.output.count(1))
    // val cc = peek(c.io.output.done)
    // println(s"SingleCounters at $a, $b, (want $count), stop $stop done? $cc expected? $done because ${(count + c.par*stride + gap)} satmode $saturate")
    // if (cc != done) println("           ERROR!!!!!!!!!!!!!! \n\n")

    // Check signal values
    expect(c.io.output.done, done)
    // expect(c.io.output.count, numEnabledCycles % c.depth)
  }

  poke(c.io.input.dir, 1)
  (0 until c.lanes).foreach{i => poke(c.io.input.enables(i), 0)}
  step(5)
  poke(c.io.input.reset, 1)
  step(1)
  poke(c.io.input.reset, 0)
  step(1)

  for (i <- 1 until 20) {
    // Generate enable vector
    val ens = (0 until c.lanes).map{i => rnd.nextInt(2)}
    testOneStep(ens)
  }

  // Test stall
  for (i <- 1 until 5) {
    val ens = (0 until c.lanes).map{i => 0}
    testOneStep(ens)
  }

  // Continue
  for (i <- 1 until c.depth) {
    // Generate enable vector
    val ens = (0 until c.lanes).map{i => rnd.nextInt(2)}
    testOneStep(ens)
  }

  // Reset and go again
  numEnabledCycles = 0
  poke(c.io.input.reset, 1)
  step(1)
  poke(c.io.input.reset, 0)
  for (i <- 1 until c.depth) {
    // Generate enable vector
    val ens = (0 until c.lanes).map{i => rnd.nextInt(2)}
    testOneStep(ens)
  }

}


class CounterTests(c: Counter) extends PeekPokeTester(c) {

  // Test triple nested counter
  val depth = 3
  var numEnabledCycles = 0
  // var expectedCounts = List(0,0,0)
  // var expectedDones = List(0,0,0)

  val gap = List(0,0,0)
  val stops = List(List(10,12,15), List(11,13,16), List(12,14,50))
  val strides = List(List(3,3,3),List(1,1,1), List(3, 4, 5))
  val start = List(0,0,0) // TODO: Test new starts
  var enable = 1
  var saturate = 1

  step(1)
  reset(1)

  stops.foreach { stop => 
  strides.foreach { stride => 
    // println("------" + stop + "--------" + stride)
    val alignedMax = stop.zip(stride).zip(c.par.reverse).map {case ((m,s),p) => 
      if (m % (s*p) == 0) m else m - (m % (s*p)) + (s*p) 
    }
    numEnabledCycles = 0
    val stepSizes = c.par.reverse.zipWithIndex.map{case (p,i) => p*stride(i) + gap(i)}
    val totalTicks = alignedMax.reduce{_*_} / stepSizes.reduce{_*_}

    def testOneStep() = {
        step(1)
        if (enable == 1) numEnabledCycles += 1
        val expectedCksum = numEnabledCycles 
        val done = if (numEnabledCycles == stop.reduce{_*_}) 1 else 0

        c.par.reverse.zipWithIndex.foreach{ case (p,ii) => 
          val i = c.par.length - ii - 1
          val ticksToInc = (alignedMax.take(i+1).reduce{_*_} * stepSizes(i)) / (alignedMax(i) * stepSizes.take(i+1).reduce{_*_})
          val period = ticksToInc*alignedMax(i) / stepSizes(i)
          val increments = (numEnabledCycles) / ticksToInc
          val base = if (saturate == 1) {
            if (numEnabledCycles >= totalTicks) {
              alignedMax(i) - c.par(ii)*stride(i)
            } else {
              (increments * stepSizes(i)) % alignedMax(i)
            }
          } else {
            increments % alignedMax(i) // TODO: Not sure if this is correct, only testing saturating ctrs now
          }
          val ctrAddr = c.par.take(ii+1).reduce{_+_} - c.par(ii)
          (0 until c.par(ii)).foreach{ k => 
            val test = peek(c.io.output.counts(ctrAddr+k))
            val expected = base + k*stride(i)
//             if (test != base + k*stride(i)) {
//               println(s"""Step ${numEnabledCycles}: (checking ctr${i}.${k} @ ${ctrAddr+k} (hw: ${test} =? ${base+k*stride(i)})
//   tic each ${ticksToInc} from ${alignedMax.take(i+1).reduce{_*_}} / ${alignedMax(i)}), 
//     increments = ${increments}
//       base = ${base} (incs % ${alignedMax(i)})
// """)
//             }
            // if (test != expected) println("WRONG!")
            // println("[stat] counter " + {ctrAddr + k} + " is " + test + " but expected " + expected + "(" + base + "+" + k + "*" + stride(i) + ")")
            expect(c.io.output.counts(ctrAddr+k), expected)
          }
        }
        // println("")
      }

      enable = 0
      poke(c.io.input.enable, enable)
      c.io.input.starts.zipWithIndex.foreach{ case (wire, i) => poke(wire,start(start.length - 1 -i)) }
      step(5)
      c.io.input.stops.zipWithIndex.foreach{ case (wire, i) => poke(wire,stop(stop.length - 1 - i)) }
      c.io.input.strides.zipWithIndex.foreach{ case (wire, i) => poke(wire,stride(stride.length - 1 - i)) }
      poke(c.io.input.reset, 1)
      step(1)
      enable = 1
      poke(c.io.input.enable, enable)
      poke(c.io.input.saturate, saturate)
      poke(c.io.input.reset, 0)

      // for (i <- 0 until (totalTicks*1.1).toInt) {
      for (i <- 0 until (20).toInt) {
        testOneStep
      }

      enable = 0
      poke(c.io.input.enable, enable)
      poke(c.io.input.reset, 1)
      step(1)
      reset(1)
      poke(c.io.input.reset, 0)

    }
  }


}


// class SingleCounterTester extends ChiselFlatSpec {
//   behavior of "SingleCounter"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new SingleCounter(3))(c => new SingleCounterTests(c)) should be (true)
//     }
//   }
// }

// class CounterTester extends ChiselFlatSpec {
//   behavior of "Counter"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new Counter(List(2,2,2)))(c => new CounterTests(c)) should be (true)
//     }
//   }
// }

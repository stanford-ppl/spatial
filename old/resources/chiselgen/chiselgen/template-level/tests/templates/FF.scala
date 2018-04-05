// See LICENSE.txt for license details.
package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}


/**
 * FF test harness
 */
class FFTests(c: FF) extends PeekPokeTester(c) {
  val initval = 10
  poke(c.io.input(0).init, initval)
  step(1)
  reset(1)
  expect(c.io.output.data, initval)

  // overwrite init
  poke(c.io.input(0).data, 0)
  poke(c.io.input(0).enable, 1)
  step(1)
  expect(c.io.output.data, 0)
  step(1)

  val numCycles = 15
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data)
    poke(c.io.input(0).data, i)
    poke(c.io.input(0).enable, newenable)
    step(1)
    if (newenable == 1) {
      // val a = peek(c.io.output.data)
      // println(s"expect $a to be $i")
      expect(c.io.output.data, i)
    } else {
      // val a = peek(c.io.output.data)
      // println(s"expect $a to be $oldout")
      expect(c.io.output.data, oldout)
    }
  }
  poke(c.io.input(0).reset, 1)
  poke(c.io.input(0).enable, 0)
  // val b = peek(c.io.output.data)
  // println(s"expect $b to be $initval")
  expect(c.io.output.data, initval)
  step(1)
  // val cc = peek(c.io.output.data)
  // println(s"expect $cc to be $initval")
  expect(c.io.output.data, initval)
  poke(c.io.input(0).reset, 0)
  step(1)
  // val d = peek(c.io.output.data)
  // println(s"expect $d to be $initval")
  expect(c.io.output.data, initval)
}

class NBufFFTests(c: NBufFF) extends PeekPokeTester(c) {
  val timeout = 400
  val initval = 1 //(0 until c.numBufs).map { i => i+1}
  var stageActives = Array.tabulate(c.numBufs) { i => 0 }
  val latencies = (0 until c.numBufs).map { i => math.abs(rnd.nextInt(15)) + 5 } 
  val shortestLatency = latencies.min
  poke(c.io.input(0).init, initval)
  var stageCounts = Array.tabulate(c.numBufs) { i => 0 }
  var stagesDone = 0
  step(5)

  def executeStage(s: Int) {
    // println(s" Stage $s active count ${stageCounts(s)}, numcicles $numCycles")
    if (stageActives(s) == 1) stageCounts(s) += 1 else stageCounts(s) = 0
    if (stageCounts(s) == latencies(s)) {
      poke(c.io.sDone(s), 1)
      poke(c.io.sEn(s), 0)
    } else if (stageCounts(s) == latencies(s) + 1) {
      poke(c.io.sEn(s), 0)
      poke(c.io.sDone(s), 0)
      stageCounts(s) = 0
      stagesDone = stagesDone + 1
      stageActives(s) = 0
    } else {
      poke(c.io.sDone(s), 0)
    }
    step(1)
    poke(c.io.sDone(s), 0)

  }
  def handleStageEnables = {
    (0 until c.numBufs).foreach { i => 
      executeStage(i)
    }
  }
  def handleBcast(expected: Int) {
    (0 until c.numBufs).foreach { i => 
      expect(c.io.output(i).data, expected)
    }
  }
  def write(data: Int = 0) {
    poke(c.io.input(0).data, data)
    poke(c.io.input(0).enable, 1)
    step(1)
    poke(c.io.input(0).enable, 0)
  }
  def read(stage: Int, data: Int) {
    (0 until c.numBufs).foreach { i => 
      val gold = if (data - i < 0) 0 else data - i
      // val a = peek(c.io.output(i).data)
      // println(s"Expecting stage $i to report $gold, reporting $a (${a == gold})")
      expect(c.io.output(i).data, gold)
    }

  }
  def rotate(x:List[Int], i:Int) = {x.drop(i)++x.take(i)}

  var numCycles = 0
  var data = 0
  for (k <- 1 until 10) { // run 10 writes
    numCycles = 0
    stagesDone = 0
    (0 until c.numBufs).foreach{ i => 
      poke(c.io.sEn(i), 1)
      stageActives(i) = 1 
    }
    val stage = (k - 1) % c.numBufs
    write(k)
    var cyclesChecking = 0
    while (!(stagesDone == c.numBufs) & numCycles < timeout) {
      if (cyclesChecking < shortestLatency) read(stage, k)
      handleStageEnables
      numCycles = numCycles+1
      cyclesChecking = cyclesChecking+1
    }
    
    step(5)
  }
  (0 until c.numBufs).foreach{ i => 
    poke(c.io.sEn(i), 0)
  }

  if ( (numCycles > timeout) | (numCycles < 2) ) {
    expect(c.io.output(0).data, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }

  // Check broadcast system
  poke(c.io.broadcast.enable, 1)
  poke(c.io.broadcast.data, 666)
  step(1)
  poke(c.io.broadcast.enable,0)
  step(1)
  for (k <- 0 until c.numBufs) {
    numCycles = 0
    stagesDone = 0
    while (!(stagesDone == c.numBufs) & numCycles < timeout) {
      handleBcast(666)
      handleStageEnables
      numCycles = numCycles+1
    }
    step(5)
  }

  if ( (numCycles > timeout) | (numCycles < 2) ) {
    expect(c.io.output(0).data, 999) // TODO: Figure out how to "expect" signals that are not hw IO
  }


  step(5)
}

class FFNoInitTests(c: FFNoInit) extends PeekPokeTester(c) {
  step(1)
  reset(1)

  // overwrite init
  poke(c.io.input.data, 0)
  poke(c.io.input.enable, 1)
  step(1)
  expect(c.io.output.data, 0)
  step(1)

  val numCycles = 15
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data)
    poke(c.io.input.data, i)
    poke(c.io.input.enable, newenable)
    step(1)
    if (newenable == 1) expect(c.io.output.data, i) else expect(c.io.output.data, oldout)
  }
}

class FFNoInitNoResetTests(c: FFNoInitNoReset) extends PeekPokeTester(c) {
  step(1)
  reset(1)

  // overwrite init
  poke(c.io.input.data, 0)
  poke(c.io.input.enable, 1)
  step(1)
  expect(c.io.output.data, 0)
  step(1)

  val numCycles = 15
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data)
    poke(c.io.input.data, i)
    poke(c.io.input.enable, newenable)
    step(1)
    if (newenable == 1) expect(c.io.output.data, i) else expect(c.io.output.data, oldout)
  }
}

class FFNoResetTests(c: FFNoReset) extends PeekPokeTester(c) {
  step(1)
  reset(1)

  // overwrite init
  poke(c.io.input.data, 0)
  poke(c.io.input.enable, 1)
  step(1)
  expect(c.io.output.data, 0)
  step(1)

  val numCycles = 15
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data)
    poke(c.io.input.data, i)
    poke(c.io.input.enable, newenable)
    step(1)
    if (newenable == 1) expect(c.io.output.data, i) else expect(c.io.output.data, oldout)
  }
}

class TFFTests(c: TFF) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  expect(c.io.output.data, 0)
  val numCycles = 20
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data)
    poke(c.io.input.enable, newenable)
    step(1)
    val now = peek(c.io.output.data)
    // Stupid hack because peeking a boolean returns a BigInt which cannot be sliced
    //  and ~0 = -1 and ~1 = -2
    if (newenable == 1 & oldout == 1) {
      expect(c.io.output.data, 0)
    } else if (newenable == 1 & oldout == 0) {
      expect(c.io.output.data, 1)      
    } else {
      expect(c.io.output.data, oldout)
    }
  }
}

class SRFFTests(c: SRFF) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  poke(c.io.input.asyn_reset, 1)
  expect(c.io.output.data, 0)
  step(1)
  expect(c.io.output.data, 0)
  poke(c.io.input.asyn_reset, 0)
  step(1)
  expect(c.io.output.data, 0)

  val numCycles = 20
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(3)
    val oldout = peek(c.io.output.data)
    newenable match {
      case 0 => 
        poke(c.io.input.reset, 1)
        poke(c.io.input.set, 0)
      case 1 => 
        poke(c.io.input.reset, 0)
        poke(c.io.input.set, 1)      
      case 2 =>
        poke(c.io.input.reset, 0)
        poke(c.io.input.set, 0)
    }

    step(1)

    newenable match {
      case 0 => 
        expect(c.io.output.data, 0)
      case 1 => 
        expect(c.io.output.data, 1)
      case 2 =>
        expect(c.io.output.data, oldout)
    }
  }

  poke(c.io.input.asyn_reset, 1)
  poke(c.io.input.set, 0)
  poke(c.io.input.reset, 0)
  expect(c.io.output.data, 0)
  step(1)
  expect(c.io.output.data, 0)
  poke(c.io.input.asyn_reset, 0)
  step(1)
  expect(c.io.output.data, 0)

}

// class FFTester extends ChiselFlatSpec {
//   behavior of "FF"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new FF(32))(c => new FFTests(c)) should be (true)
//     }
//   }
// }

// class FFNoInitTester extends ChiselFlatSpec {
//   behavior of "FFNoInit"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new FFNoInit(32))(c => new FFNoInitTests(c)) should be (true)
//     }
//   }
// }

// class FFNoInitNoResetTester extends ChiselFlatSpec {
//   behavior of "FFNoInit"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new FFNoInit(32))(c => new FFNoInitTests(c)) should be (true)
//     }
//   }
// }

// class FFNoResetTester extends ChiselFlatSpec {
//   behavior of "FFNoInit"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new FFNoInit(32))(c => new FFNoInitTests(c)) should be (true)
//     }
//   }
// }

// class TFFTester extends ChiselFlatSpec {
//   behavior of "TFF"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new TFF())(c => new TFFTests(c)) should be (true)
//     }
//   }
// }

// class SRFFTester extends ChiselFlatSpec {
//   behavior of "SRFF"
//   backends foreach {backend =>
//     it should s"correctly add randomly generated numbers $backend" in {
//       Driver(() => new SRFF())(c => new SRFFTests(c)) should be (true)
//     }
//   }
// }

// See LICENSE.txt for license details.
package templates

import chisel3._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import chisel3.testers.BasicTester
import org.scalatest._
import org.scalatest.prop._

/**
 * Mem1D test harness
 */
class Mem1DTests(c: Mem1D) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  for (i <- 0 until c.size ) {
    poke(c.io.w.addr, i)
    poke(c.io.w.data, i*2)
    poke(c.io.w.en, 1)
    step(1) 
    poke(c.io.w.en, 0)
    step(1)
  }

  for (i <- 0 until c.size ) {
    poke(c.io.r.addr, i)
    poke(c.io.r.en, 1)
    step(1)
    expect(c.io.output.data, i*2)
    poke(c.io.r.en, 0)
    step(1)
  }

}

/**
 * MemND test harness
 */
class MemNDTests(c: MemND) extends PeekPokeTester(c) {
  val depth = c.dims.reduce{_*_}
  val N = c.dims.length

  step(1)
  reset(1)
  // poke(c.io.wMask, 1) // Do not mask at all when testing this template directly
  // poke(c.io.rMask, 1) // Do not mask at all when testing this template directly
  // Assume only 2D
  for (i <- 0 until c.dims(0) ) {
    for (j <- 0 until c.dims(1) ) {
      c.io.w.addr.zip(List(i,j)).foreach { case (port, addr) => poke(port, addr) }
      poke(c.io.w.data, (i*c.dims(0) + j)*2)
      poke(c.io.w.en, 1)
      poke(c.io.wMask, 1)
      step(1) 
      poke(c.io.w.en, 0)
      poke(c.io.wMask, 0)
      step(1)
    }
  }

  for (i <- 0 until c.dims(0) ) {
    for (j <- 0 until c.dims(1) ) {
      c.io.r.addr.zip(List(i,j)).foreach { case (port, addr) => poke(port, addr) }
      poke(c.io.r.en, 1)
      poke(c.io.rMask, 1)
      step(1)
      // Console.println(s"Expect ${2*(i*c.dims(0) + j)} but got ${peek(c.io.output.data)}")
      expect(c.io.output.data, 2*(i*c.dims(0) + j))
      poke(c.io.r.en, 0)
      poke(c.io.rMask, 0)
      step(1)
    }
  }

}


/**
 * SRAM test harness
 */
class SRAMTests(c: SRAM) extends PeekPokeTester(c) {
  val depth = c.logicalDims.reduce{_*_}
  val N = c.logicalDims.length

  reset(1)

  // Write to each address
  for (i <- 0 until c.logicalDims(0)) { // Each row
    for (j <- 0 until c.logicalDims(1) by c.wPar(0)) {
      // Set addrs
      var idx = 0
      (0 until c.wPar.length).foreach{ writer => 
        (0 until c.wPar(writer)).foreach { kdim => 
          poke(c.io.w(idx).addr(0), i)
          poke(c.io.w(idx).addr(1), j+kdim)
          poke(c.io.w(idx).data, (i*c.logicalDims(0) + j + kdim)*2)
          if (writer == 0) {
            poke(c.io.w(idx).en, true)
          } else {
            poke(c.io.w(idx).en, false)
          }
          idx = idx + 1
        }
      }
      step(1)
    }
  }
  // Turn off wEn
  (0 until c.wPar.reduce{_+_}).foreach{ wbundle => 
    poke(c.io.w(wbundle).en, false)
  }

  step(30)

  // Check each address
  for (i <- 0 until c.logicalDims(0)) { // Each row
    for (j <- 0 until c.logicalDims(1) by c.rPar(0)) {
      // Set addrs
      var idx = 0
      (0 until c.rPar.length).foreach{ reader => 
        (0 until c.rPar(reader)).foreach { kdim => 
          poke(c.io.r(idx).addr(0), i)
          poke(c.io.r(idx).addr(1), j+kdim)
          if (reader == 0) {
            poke(c.io.r(idx).en, true)
          } else {
            poke(c.io.r(idx).en, false)
          }
          idx = idx + 1
        }
      }
      step(1)
      (0 until c.rPar(0)).foreach { kdim => 
        expect(c.io.output.data(kdim), (i*c.logicalDims(0) + j + kdim)*2)
      }
    }
  }
  // Turn off rEn
  (0 until c.rPar.reduce{_+_}).foreach{ reader => 
    poke(c.io.r(reader).en, false)
  }

  step(1)


}


/**
 * SRAM test harness
 */
class NBufSRAMTests(c: NBufSRAM) extends PeekPokeTester(c) {

  val timeout = 400
  val initvals = (0 until c.numBufs).map { i => i+1}
  var stageActives = Array.tabulate(c.numBufs) { i => 0 }
  val latencies = (0 until c.numBufs).map { i => math.abs(rnd.nextInt(15)) + 5 } 
  var stageCounts = Array.tabulate(c.numBufs) { i => 0 }
  var stagesDone = 0

  reset(1)

  def fillSRAM(wPort: Int, dat: Int) {

    // Write to each address
    for (i <- 0 until c.logicalDims(0)) { // Each row
      for (j <- 0 until c.logicalDims(1) by c.wPar(0)) {
        // Set addrs
        var idx = 0
        (0 until c.wPar.length).foreach{ writer => 
          (0 until c.wPar(writer)).foreach { kdim => 
            poke(c.io.w(idx).addr(0), i)
            poke(c.io.w(idx).addr(1), j+kdim)
            poke(c.io.w(idx).data, 1000*dat + i*c.logicalDims(0) + j + kdim)
            if (writer == 0) {
              poke(c.io.w(idx).en, true)
            } else {             
              poke(c.io.w(idx).en, false)
            }
            idx = idx + 1
          }
        }
        step(1)
      }
    }
    // Turn off wEn
    (0 until c.wPar.reduce{_+_}).foreach{ writer => 
      poke(c.io.w(writer).en, false)
    }

    step(30)
  }
  def broadcastFillSRAM(dat: Int) {

    // Write to each address
    for (i <- 0 until c.logicalDims(0)) { // Each row
      for (j <- 0 until c.logicalDims(1) by c.bPar.head) {
        // Set addrs
        (0 until c.bPar.head).foreach { kdim => 
          poke(c.io.broadcast(kdim).addr(0), i)
          poke(c.io.broadcast(kdim).addr(1), j+kdim)
          poke(c.io.broadcast(kdim).data, dat + i*c.logicalDims(0) + j + kdim)
          poke(c.io.broadcast(kdim).en, true)
        }
        step(1)
      }
    }
    // Turn off wEn
    (0 until c.bPar.head).foreach {kdim =>
      poke(c.io.broadcast(kdim).en, false)
    }
    
    step(30)
  }

  def readSRAM(rPort: Int, dat: Int, base: Int = 1000) {

    // Read at each address
    for (i <- 0 until c.logicalDims(0)) { // Each row
      for (j <- 0 until c.logicalDims(1) by c.rPar(0)) {
        // Set addrs
        var idx = 0
        (0 until c.rPar.length).foreach{ readers => 
          (0 until c.rPar(readers)).foreach { kdim => 
            poke(c.io.r(idx).addr(0), i)
            poke(c.io.r(idx).addr(1), j+kdim)
            if (readers == 0) {
              poke(c.io.r(idx).en, true)
            } else {
              poke(c.io.r(idx).en, false)
            }
            idx = idx + 1
          }
        }
        step(1)
        (0 until c.rPar.max).foreach {kdim => 
          val gold = base*dat + i*c.logicalDims(0) + j + kdim
          // val a = peek(c.io.output.data(rPort*c.rPar.max + kdim))
          // println(s"Expecting $gold but got $a (${a == gold}) on port $rPort")
          expect(c.io.output.data(rPort*c.rPar.max + kdim), gold)
        }
      }
    }
    // Turn off wEn
    (0 until c.rPar.reduce{_+_}).foreach{ reader => 
      poke(c.io.r(reader).en, false)
    }

    step(30)

  }

  def executeStage(s: Int) {
    // println(s" Stage $s active count ${stageCounts(s)}, numcicles $numCycles")
    if (stageActives(s) == 1) stageCounts(s) += 1 else stageCounts(s) = 0
    if (stageCounts(s) == latencies(s)) {
      poke(c.io.sDone(s), 1)
    } else if (stageCounts(s) == latencies(s) + 1) {
      poke(c.io.sEn(s), 0)
      poke(c.io.sDone(s), 0)
      stageCounts(s) = 0
      stagesDone = stagesDone + 1
      stageActives(s) = 0
    } else {
      poke(c.io.sDone(s), 0)
    }
  }
  def handleStageEnables = {
    (0 until c.numBufs).foreach { i => 
      executeStage(i)
    }
  }

  var numCycles = 0
  var iter = 1
  var writingPort = 0
  var readingPort = c.numBufs-1
  for (k <- 0 until c.numBufs*5) { 
    numCycles = 0
    stagesDone = 0
    (0 until c.numBufs).foreach{ i => 
      poke(c.io.sEn(i), 1)
      stageActives(i) = 1 
    }
    fillSRAM(writingPort, iter)
    if (iter >= c.numBufs) readSRAM(readingPort, iter-c.numBufs+1)
    while (!(stagesDone == c.numBufs) & numCycles < timeout) {
      handleStageEnables
      step(1)
      numCycles = numCycles+1
    }
    iter += 1

    step(5)
  }

  // test broadcast
  broadcastFillSRAM(20)
  for (k <- 0 until c.numBufs) { 
    numCycles = 0
    stagesDone = 0
    (0 until c.numBufs).foreach{ i => 
      poke(c.io.sEn(i), 1)
      stageActives(i) = 1 
    }
    readSRAM(readingPort, 20, 1)
    while (!(stagesDone == c.numBufs) & numCycles < timeout) {
      handleStageEnables
      step(1)
      numCycles = numCycles+1
    }
    iter += 1

    step(5)
  }
 


  step(5)
}

// class Mem1DTester extends ChiselFlatSpec {
//   behavior of "Mem1D"
//   backends foreach {backend =>
//     it should s"correctly do $backend" in {
//       Driver(() => new Mem1D(1024))(c => new Mem1DTests(c)) should be (true)
//     }
//   }
// }

// class MemNDTester extends ChiselFlatSpec {
//   behavior of "MemND"
//   backends foreach {backend =>
//     it should s"correctly do $backend" in {
//       Drivera(() => new MemND(List(4,8)))(c => new MemNDTests(c)) should be (true)
//     }
//   }
// }

// class SRAMTester extends ChiselFlatSpec {
//   behavior of "SRAM"
//   backends foreach {backend =>
//     it should s"correctly do $backend" in {
//       Driver(() => new SRAM(List(16,16), 32, 
//                               List(1,2), List(1,1), 1, 1,
//                               2, 2, "strided"))(c => new SRAMTests(c)) should be (true)
//     }
//   }
// }

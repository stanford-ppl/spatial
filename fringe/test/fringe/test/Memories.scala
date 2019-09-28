// See LICENSE.txt for license details.
package fringe.test

import chisel3._
import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import chisel3.testers.BasicTester
import fringe.templates.memory._

import org.scalatest._
import org.scalatest.prop._

/**
 * Mem1D test harness
 */
class Mem1DTests(c: Mem1D) extends PeekPokeTester(c) {
  step(1)
  reset(1)
  for (i <- 0 until c.size ) {
    poke(c.io.w.ofs.head, i)
    poke(c.io.w.data.head, i*2)
    poke(c.io.w.en.head, 1)
    poke(c.io.wMask, 1)
    step(1) 
    poke(c.io.w.en.head, 0)
    poke(c.io.wMask, 0)
    step(1)
  }

  for (i <- 0 until c.size ) {
    poke(c.io.r.ofs.head, i)
    poke(c.io.r.en.head, 1)
    poke(c.io.rMask, 1)
    step(1)
    expect(c.io.output.data, i*2)
    poke(c.io.r.en.head, 0)
    poke(c.io.rMask, 0)
    step(1)
  }

}

class FFTests(c: FF) extends PeekPokeTester(c) {
  val initval = 10
  poke(c.io.xBarW(0).init.head, initval)
  poke(c.io.xBarW(0).reset.head, 1)
  reset(1)
  step(1)
  expect(c.io.output.data(0), initval)
  poke(c.io.xBarW(0).reset.head, 0)
  step(1)

  // overwrite init
  poke(c.io.xBarW(0).data.head, 0)
  poke(c.io.xBarW(0).en.head, 1)
  step(1)
  expect(c.io.output.data(0), 0)
  step(1)

  val numCycles = 15
  for (i <- 0 until numCycles) {
    val newenable = rnd.nextInt(2)
    val oldout = peek(c.io.output.data(0))
    poke(c.io.xBarW(0).data.head, i)
    poke(c.io.xBarW(0).en.head, newenable)
    step(1)
    if (newenable == 1) {
      // val a = peek(c.io.output.data(0))
      // println(s"expect $a to be $i ? ${a == i} branch a")
      expect(c.io.output.data(0), i)
    } else {
      // val a = peek(c.io.output.data(0))
      // println(s"expect $a to be $oldout ? ${a == oldout} branch b")
      expect(c.io.output.data(0), oldout)
    }
  }
  poke(c.io.xBarW(0).reset.head, 1)
  poke(c.io.xBarW(0).en.head, 0)
  // val b = peek(c.io.output.data(0))
  // println(s"expect $b to be $initval ? ${b == initval}")
  step(1)
  expect(c.io.output.data(0), initval)
  step(1)
  // val cc = peek(c.io.output.data(0))
  // println(s"expect $cc to be $initval ? ${cc == initval}")
  expect(c.io.output.data(0), initval)
  poke(c.io.xBarW(0).reset.head, 0)
  step(1)
  // val d = peek(c.io.output.data(0))
  // println(s"expect $d to be $initval ? ${d == initval}")
  expect(c.io.output.data(0), initval)
}

class ShiftRegFileTests(c: ShiftRegFile) extends PeekPokeTester(c) {


  if (!c.p.xBarWMux.values.head._2.isDefined) { // RegFile mode
    // Fill regfile
    for (i <- 0 until c.p.Ds(0)) {
      for (j <- 0 until c.p.Ds(1)) {
        poke(c.io.xBarW(0).banks(0), i)
        poke(c.io.xBarW(0).banks(1), j)
        poke(c.io.xBarW(0).data.head, i+j)
        poke(c.io.xBarW(0).en.head, 1)
        step(1)
      }
    }
    poke(c.io.xBarW(0).en.head, 0)
    step(1)

    // Read RegFile
    for (i <- 0 until c.p.Ds(0)) {
      for (j <- 0 until c.p.Ds(1)) {
        poke(c.io.xBarR(0).banks(0), i)
        poke(c.io.xBarR(0).banks(1), j)
        poke(c.io.xBarR(0).en.head, 1)
        expect(c.io.output.data(0), i+j)
        step(1)
      }
    }
  }
  else { // Shift mode
    for (wavefront <- 0 until c.p.Ds(1)) {
      for (i <- 0 until c.p.Ds(0)) {
        poke(c.io.xBarW(0).banks(0), i)
        poke(c.io.xBarW(0).banks(1), 0)
        poke(c.io.xBarW(0).data.head, i+wavefront*10)
        poke(c.io.xBarW(0).shiftEn.head, 1)
        poke(c.io.xBarW(0).en.head, 1)
        step(1)
      }

      poke(c.io.xBarW(0).en.head, 0)
      poke(c.io.xBarW(0).shiftEn.head, 0)
      step(1)


      println("Shift: " + wavefront)
      for (i <- 0 until c.p.Ds(0)) {
        for (j <- 0 until c.p.Ds(1)) {
          poke(c.io.xBarR(0).banks(0), i)
          poke(c.io.xBarR(0).banks(1), j)
          poke(c.io.xBarR(0).en.head, 1)
          // if (j < wavefront) expect(c.io.output.data(i), 0)
          // else expect(c.io.output.data(i), 0)
          val d = peek(c.io.output.data(0))
          val g = 0 max {i + wavefront*10 - j*10}
          expect(c.io.output.data(0), g)
          step(1)
          // print(" " + d + "(" + g + ")")
        }
        println("")
      }
      println("-------------------")

    }
  }

}

/**
 * SRAM test harness
 */
class BankedSRAMTests(c: BankedSRAM) extends PeekPokeTester(c) {
  val depth = c.p.Ds.reduce{_*_}
  val N = c.p.Ds.length

  reset(1)

  // Write to each address
  val wPar = c.p.directWMux.values.toList.head._1.length
  for (i <- 0 until c.p.Ds(0) by c.p.Ns(0)) { // Each row
    for (j <- 0 until c.p.Ds(1) by c.p.Ns(1)) {
      // Set addrs
      (0 until c.p.Ns(0)).foreach{ ii => (0 until c.p.Ns(1)).foreach{ jj =>
        val kdim = ii * c.p.Ns(1) + jj
        // poke(c.io.directW(kdim).banks(0), i % c.p.banks(0))
        // poke(c.io.directW(kdim).banks(1), (j+kdim) % c.p.banks(1))
        poke(c.io.directW.head.ofs(kdim), (i+ii) / c.p.Ns(0) * (c.p.Ds(1) / c.p.Ns(1)) + (j+jj) / c.p.Ns(1))
        poke(c.io.directW.head.data(kdim), (i*c.p.Ds(0) + j + kdim)*2)
        poke(c.io.directW.head.en(kdim), true)
      }}
      step(1)
    }
  }
  // Turn off wEn
  (0 until wPar).foreach{ kdim => 
    poke(c.io.directW.head.en(kdim), false)
  }

  step(30)

  // Check each address
  val rPar = c.p.directRMux.values.toList.head._1.length
  for (i <- 0 until c.p.Ds(0) by c.p.Ns(0)) { // Each row
    for (j <- 0 until c.p.Ds(1) by c.p.Ns(1)) {
      // Set addrs
      (0 until c.p.Ns(0)).foreach{ ii => (0 until c.p.Ns(1)).foreach{ jj =>
        val kdim = ii * c.p.Ns(1) + jj
        // poke(c.io.directR(kdim).banks(0), i % c.p.banks(0))
        // poke(c.io.directR(kdim).banks(1), (j+kdim) % c.p.banks(1))
        poke(c.io.directR.head.ofs(kdim), (i+ii) / c.p.Ns(0) * (c.p.Ds(1) / c.p.Ns(1)) + (j+jj) / c.p.Ns(1))
        poke(c.io.directR.head.en(kdim), true)
      }}
      step(1)
      (0 until rPar).foreach { kdim => 
        expect(c.io.output.data(kdim), (i*c.p.Ds(0) + j + kdim)*2)
      }
    }
  }
  // Turn off rEn
  (0 until rPar).foreach{ reader => 
    poke(c.io.directR.head.en(reader), false)
  }

  step(1)


}


/**
 * SRAM test harness
 */
class NBufMemTests(c: NBufMem) extends PeekPokeTester(c) {

  val depth = c.logicalDims.reduce{_*_}
  val N = c.logicalDims.length

  reset(1)

  // Broadcast
  c.mem match {
    case BankedSRAMType => 
      for (i <- 0 until c.logicalDims(0) by c.banks(0)) {
        for (j <- 0 until c.logicalDims(1) by c.banks(1)) {
          (0 until c.banks(0)).foreach{ ii => (0 until c.banks(1)).foreach{ jj =>
            poke(c.io.broadcastW(0).banks(0), ii)
            poke(c.io.broadcastW(0).banks(1), jj)
            poke(c.io.broadcastW(0).ofs.head, (i+ii) / c.banks(0) * (c.logicalDims(1) / c.banks(1)) + (j+jj) / c.banks(1))
            poke(c.io.broadcastW(0).data.head, 999)
            poke(c.io.broadcastW(0).en.head, true)
            step(1)      
          }}
        }
      }
    case FFType => 
      poke(c.io.broadcastW(0).data.head, 999)
      poke(c.io.broadcastW(0).en.head, true)
      step(1)
    case ShiftRegFileType => 
      for (i <- 0 until c.logicalDims(0)) {
        for (j <- 0 until c.logicalDims(1)) {
          poke(c.io.broadcastW(0).banks(0), i)
          poke(c.io.broadcastW(0).banks(1), j)
          poke(c.io.broadcastW(0).data.head, 999)
          poke(c.io.broadcastW(0).en.head, true)
          step(1)      
        }
      }
    case FIFOType =>
    case LIFOType =>
    case LineBufferType => 
    case FIFORegType =>
  }
  c.io.broadcastW.foreach{p => poke(p.en.head, false)}
  step(1)

  // Read all bufs
  for (buf <- 0 until c.numBufs) {
    println("Buffer: " + buf)
    c.mem match {
      case BankedSRAMType => 
        val rPar = c.directRMux.values.toList.head.values.toList.map(_._1).flatten.length
        val base = c.directRMux.keys.toList.head * rPar
        for (i <- 0 until c.logicalDims(0) by c.banks(0)) { // Each row
          for (j <- 0 until c.logicalDims(1) by c.banks(1)) {
            // Set addrs
            (0 until c.banks(0)).foreach{ ii => (0 until c.banks(1)).foreach{ jj =>
              val kdim = ii * c.banks(1) + jj
              // poke(c.io.directR(kdim).banks(0), i % c.banks(0))
              // poke(c.io.directR(kdim).banks(1), (j+kdim) % c.banks(1))
              poke(c.io.directR.head.ofs(kdim), (i+ii) / c.banks(0) * (c.logicalDims(1) / c.banks(1)) + (j+jj) / c.banks(1))
              poke(c.io.directR.head.en(kdim), true)
            }}
            step(1)
            (0 until rPar).foreach { kdim => 
              expect(c.io.output.data(kdim), 999)
            }
          }
        }  
      case FFType => 
        val base = c.xBarRMux.keys.toList.head
        expect(c.io.output.data(0), 999)
        step(1)
      case ShiftRegFileType => 
        for (i <- 0 until c.logicalDims(0)) {
          for (j <- 0 until c.logicalDims(1)) {
            poke(c.io.xBarR(0).banks(0), i)
            poke(c.io.xBarR(0).banks(1), j)
            poke(c.io.xBarR(0).en.head, true)
            expect(c.io.output.data(0),999)
            val x = peek(c.io.output.data(0))
            print(" " + x)
            step(1)      
          }
          println("")
        }
      case FIFOType =>
      case LIFOType =>
      case LineBufferType => 
      case FIFORegType =>
    }
    c.io.directR.foreach{p => poke(p.en.head, false)}
    // Rotate buffer
    poke(c.io.sEn(0), 1)
    step(1)
    poke(c.io.sDone(0), 1)
    step(1)
    poke(c.io.sEn(0), 0)
    poke(c.io.sDone(0), 0)
    step(1)
  }

  step(20)


  for (epoch <- 0 until c.numBufs*2) {
    c.mem match {
      case BankedSRAMType => 
        // Write to each address
        val wPar = c.directWMux.values.toList.head.values.toList.map(_._1).flatten.length
        for (i <- 0 until c.logicalDims(0) by c.banks(0)) { // Each row
          for (j <- 0 until c.logicalDims(1) by c.banks(1)) {
            // Set addrs
            (0 until c.banks(0)).foreach{ ii => (0 until c.banks(1)).foreach{ jj =>
              val kdim = ii * c.banks(1) + jj
              // poke(c.io.directW(kdim).banks(0), i % c.banks(0))
              // poke(c.io.directW(kdim).banks(1), (j+kdim) % c.banks(1))
              poke(c.io.directW.head.ofs(kdim), (i+ii) / c.banks(0) * (c.logicalDims(1) / c.banks(1)) + (j+jj) / c.banks(1))
              poke(c.io.directW.head.data(kdim), epoch*100 + (i*c.logicalDims(0) + j + kdim)*2)
              poke(c.io.directW.head.en(kdim), 1)
            }}
            step(1)
          }
        }
      case FFType => 
        poke(c.io.xBarW(0).data.head, epoch*100)
        poke(c.io.xBarW(0).en.head, true)
        step(1)
      case ShiftRegFileType => 
        for (i <- 0 until c.logicalDims(0)) {
          for (j <- 0 until c.logicalDims(1)) {
            poke(c.io.xBarW(0).banks(0), i)
            poke(c.io.xBarW(0).banks(1), j)
            poke(c.io.xBarW(0).data.head, epoch*100 + (i*c.logicalDims(0)+j)*2)
            poke(c.io.xBarW(0).en.head, true)
            step(1)      
          }
        }
      case FIFOType =>
      case LIFOType =>
      case LineBufferType => 
      case FIFORegType =>
    }

    // Turn off wEn
    c.io.directW.foreach{p => poke(p.en.head, false)}
    c.io.xBarW.foreach{p => poke(p.en.head, false)}

    step(30)

    // Assume write to buffer 0, read from buffer c.numBufs-1, so do the swapping
    for (i <- 0 until c.numBufs-1){
      // Rotate buffer
      poke(c.io.sEn(0), 1)
      step(1)
      poke(c.io.sDone(0), 1)
      step(1)
      poke(c.io.sEn(0), 0)
      poke(c.io.sDone(0), 0)
      step(1)
    }

    // Check each address
    c.mem match {
      case BankedSRAMType => 
        val rPar = c.directRMux.values.toList.head.values.toList.map(_._1).flatten.length
        for (i <- 0 until c.logicalDims(0) by c.banks(0)) { // Each row
          for (j <- 0 until c.logicalDims(1) by c.banks(1)) {
            // Set addrs
            (0 until c.banks(0)).foreach{ ii => (0 until c.banks(1)).foreach{ jj =>
              val kdim = ii * c.banks(1) + jj
              // poke(c.io.directR(kdim).banks(0), i % c.banks(0))
              // poke(c.io.directR(kdim).banks(1), (j+kdim) % c.banks(1))
              poke(c.io.directR.head.ofs(kdim), (i+ii) / c.banks(0) * (c.logicalDims(1) / c.banks(1)) + (j+jj) / c.banks(1))
              poke(c.io.directR.head.en(kdim), 1)
            }}
            step(1)
            (0 until rPar).foreach { kdim => 
              expect(c.io.output.data(kdim), epoch*100 + (i*c.logicalDims(0) + j + kdim)*2)
            }
          }
        }
      case FFType => 
        val base = c.xBarRMux.keys.toList.head
        expect(c.io.output.data(0), epoch*100)
        step(1)
      case ShiftRegFileType => 
        for (i <- 0 until c.logicalDims(0)) {
          for (j <- 0 until c.logicalDims(1)) {
            poke(c.io.xBarR(0).banks(0), i)
            poke(c.io.xBarR(0).banks(1), j)
            expect(c.io.output.data(0),epoch*100 + (i*c.logicalDims(0)+j)*2)
            val x = peek(c.io.output.data(0))
            print(" " + x)
            step(1)
          }
          println("")
        }
      case FIFOType =>
      case LIFOType =>
      case LineBufferType => 
      case FIFORegType =>
    }
    println("")
    
    // Turn off rEn
    c.io.directR.foreach{p => poke(p.en.head, false)}

    step(1)
  }
  


  step(5)
}


class FIFOTests(c: FIFO) extends PeekPokeTester(c) {
  reset(1)
  step(5)

  var fifo = scala.collection.mutable.Queue[Int]()
  def enq(datas: Seq[Int], ens: Seq[Int]): Unit = {
    (0 until datas.length).foreach { i => poke(c.io.xBarW.head.data(i), datas(i)) }
    (0 until datas.length).foreach { i => poke(c.io.xBarW.head.en(i), ens(i)) }
    step(1)
    (0 until datas.length).foreach { i => poke(c.io.xBarW.head.en(i), 0) }
    step(1)
    (0 until datas.length).foreach{i => if (ens(i) != 0) fifo.enqueue(datas(i))}
  }
  def deq(ens: Seq[Int]): Unit = {
    (0 until ens.length).foreach { i => poke(c.io.xBarR.head.en(i), ens(i)) }
    val num_popping = ens.reduce{_+_}
    (0 until ens.length).foreach{i => 
      val out = peek(c.io.output.data(i))
      if (ens(i) == 1) {
        println("hw has " + out + " at port " + i + ", wanted " + fifo.head)
        expect(c.io.output.data(i), fifo.dequeue())
      }
    }
    step(1)
    (0 until ens.length).foreach{ i => poke(c.io.xBarR.head.en(i),0)}
  }

  // fill FIFO halfway
  var x = 0
  var things_pushed = 0
  for (i <- 0 until c.p.volume/c.p.xBarWMux.values.head._1/2) {
    val ens = (0 until c.p.xBarWMux.values.head._1).map{i => rnd.nextInt(2)}
    val datas = (0 until c.p.xBarWMux.values.head._1).map{i => x = x + 1; x /*rnd.nextInt(5)*/}
    things_pushed = things_pushed + ens.reduce{_+_}
    enq(datas, ens)
  }

  // hold for a bit
  step(5)

  // pop FIFO halfway
  var things_popped = 0
  for (i <- 0 until c.p.volume/c.p.xBarRMux.values.head._1/2) {
    val ens = (0 until c.p.xBarRMux.values.head._1).map{i => rnd.nextInt(2)}
    things_popped = things_popped + ens.reduce{_+_}
    deq(if (things_popped > things_pushed) (0 until c.p.xBarRMux.values.head._1).map{_ => 0} else ens)
  }

  
}

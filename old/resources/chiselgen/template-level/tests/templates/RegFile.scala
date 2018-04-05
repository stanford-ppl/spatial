package templates

import chisel3.iotesters.{PeekPokeTester, Driver, ChiselFlatSpec}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.IndexedSeq
class ShiftRegFileTests(c: ShiftRegFile) extends PeekPokeTester(c) {

  poke(c.io.reset, 1)
  step(1)
  poke(c.io.reset, 0)

  val shift_plane = if (c.dims.length > 1) {c.dims.dropRight(1).reduce{_*_}} else 1
  var gold = ArrayBuffer.fill(c.dims.reduce{_*_})(0)
  val numCycles = 100
  var i = 0
  for (cycle <- 0 until numCycles) {
    // Shift random crap
    val shift_ens = (0 until c.wPar).map{i => if ( cycle > 3 ) rnd.nextInt(2) else 0}
    val new_datas = (0 until c.wPar*c.stride).map{i => cycle*c.stride + i}

    // Set addrs
    (0 until c.wPar*c.stride).foreach{i => 
      val coords = if (c.dims.length == 1) List(0) else {
        (0 until c.dims.length).map { k => 
          if (k + 1 < c.dims.length) {(i/c.stride*c.dims.last / c.dims.drop(k+1).reduce{_*_}) % c.dims(k)} else {i/c.stride*c.dims.last % c.dims(k)}
        }
      }
      (0 until c.dims.length).foreach{j => 
        poke(c.io.w(i).addr(j), coords(j))
      }
    }

    (0 until c.wPar*c.stride).foreach { i => poke(c.io.w(i).data, new_datas(i))}
    (0 until c.wPar).foreach { i => (0 until c.stride).foreach{j => poke(c.io.w(i*c.stride + j).shiftEn, shift_ens(i))}}
    step(1)
    c.io.w.foreach { port => poke(port.shiftEn,0)}
    (c.dims.reduce{_*_}-1 to 0 by -1).foreach { i =>
      val coords = (0 until c.dims.length).map { k => 
        if (k + 1 < c.dims.length) {(i / c.dims.drop(k+1).reduce{_*_}) % c.dims(k)} else {i % c.dims(k)}
      }
      // println(s"working on $i, coords $coords")
      val axis = if (c.dims.length == 1) {0} else {(i - coords.last) / c.dims.last }
      if (shift_ens(axis) == 1) {
        if (coords.last < c.stride) {
          gold(i) = new_datas(axis*c.stride + i%c.stride)
        } else {
          gold(i) = gold(i-c.stride)
        }
      }
      val newaxis = if (coords.last == 0) "\n" else ""
      val data = peek(c.io.data_out(i)) 
      val g = gold(i)
      expect(c.io.data_out(i), g)
      // print(newaxis + g + " ")
    }

    print(s"\n\nGold: ")
    (0 until gold.length).foreach{i => 
      if (i % c.dims.last == 0) print("| ")
      print(s"${gold(i)} ")
    }
    print(s"\nGot:  ")
    (0 until gold.length).foreach{i => 
      if (i % c.dims.last == 0) print("| ")
      print(s"${peek(c.io.data_out(i))} ")
    }
    step(1)
    // expect(c.io.output.data, initval)
  }
}


class NBufShiftRegFileTests(c: NBufShiftRegFile) extends PeekPokeTester(c) {
  poke(c.io.reset, 1)
  step(1)
  poke(c.io.reset, 0)

  val shift_plane = if (c.dims.length > 1) {c.dims.dropRight(1).reduce{_*_}} else 1
  var gold = ArrayBuffer.fill(c.numBufs*c.dims.reduce{_*_})(0)
  val numCycles = 200
  var i = 0
  for (cycle <- 0 until numCycles) {
    // Shift random crap
    val shift_ens = (0 until c.wPar.values.reduce{_+_}).map{i => if ( cycle > 3 ) rnd.nextInt(2) else 0}
    val new_datas = (0 until c.wPar.values.reduce{_+_}*c.stride).map{i => cycle*c.stride + i}
    // Set addrs
    (0 until c.wPar.values.reduce{_+_}*c.stride).foreach{i => 
      val coords = if (c.dims.length == 1) List(0) else {
        (0 until c.dims.length).map { k => 
          if (k + 1 < c.dims.length) {(i/c.stride*c.dims.last / c.dims.drop(k+1).reduce{_*_}) % c.dims(k)} else {i/c.stride*c.dims.last % c.dims(k)}
        }
      }
      (0 until c.dims.length).foreach{j => 
        poke(c.io.w(i).addr(j), coords(j))
      }
    }

    (0 until c.wPar.values.reduce{_+_}*c.stride).foreach { i => poke(c.io.w(i).data, new_datas(i))}
    (0 until c.wPar.values.reduce{_+_}).foreach { i => (0 until c.stride).foreach{j => poke(c.io.w(i*c.stride + j).shiftEn, shift_ens(i))}}
    step(1)
    c.io.w.foreach { port => poke(port.shiftEn,0)}
    (0 until c.numBufs).foreach { buf => 
      // println("Buffer " + buf + ":")
      val base = buf * c.dims.reduce{_*_}
      (c.dims.reduce{_*_}-1 to 0 by -1).foreach { i =>
        val coords = (0 until c.dims.length).map { k => 
          if (k + 1 < c.dims.length) {(i / c.dims.drop(k+1).reduce{_*_}) % c.dims(k)} else {i % c.dims(k)}
        }
        // println(s"working on $i, coords $coords")
        val axis = if (c.dims.length == 1) {0} else {(i - coords.last) / c.dims.last }
        if (shift_ens(axis) == 1 & buf == 0) {
          if (coords.last < c.stride) {
            gold(i) = new_datas(axis*c.stride + c.stride - 1 - (i%c.stride) )
          } else {
            gold(i) = gold(i-c.stride)
          }
        }
        val newaxis = if (coords.last == 0) "\n" else ""
        val data = peek(c.io.data_out(base + i)) 
        val g = gold(base + i)
        expect(c.io.data_out(base + i), g)
        // print(newaxis + g + " ")
      }

      print(s"\n\nGold: ")
      (0 until c.dims.reduce{_*_}).foreach{i => 
        if (i % c.dims.last == 0) print("| ")
        print(s"${gold(i)} ")
      }
      print(s"\nGot:  ")
      (0 until c.dims.reduce{_*_}).foreach{i => 
        if (i % c.dims.last == 0) print("| ")
        print(s"${peek(c.io.data_out(i))} ")
      }
      println("")
    }
    step(1)
    // println("\nSWAP!\n")
    if (cycle % (c.dims.last/2) == 0) {
      // Force buffer swap
      poke(c.io.sEn(0), 1)
      step(5)
      poke(c.io.sDone(0), 1)
      step(1)
      poke(c.io.sEn(0), 0)
      poke(c.io.sDone(0), 0)
      step(1)
      // Swap golds
      for (buf <- (c.numBufs-1) until 0 by -1){
        val base = buf*c.dims.reduce{_*_}
        for (i <- 0 until c.dims.reduce{_*_}) {
          gold(buf*c.dims.reduce{_*_}+i) = gold((buf-1)*c.dims.reduce{_*_}+i)
        }
      }
    }
    step(1)
    // expect(c.io.output.data, initval)
  }
}

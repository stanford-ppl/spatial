package spatial.tests.feature.control

import spatial.dsl._
import spatial.lib._

@test class Scan_fifofull extends SpatialTest {
  override def runtimeArgs: Args = "256"

  def main(args: Array[String]): Unit = {

    val tileSize = 64
    val N = args(0).to[Int]

    val length = ArgIn[Int]
    setArg(length, N)

    val data1        = Array.tabulate(N){i => i * 3}
    val data2        = Array.tabulate(N){i => i % 2}

    val dram1        = DRAM[Int](length)
    val dram2        = DRAM[Int](length)
    val outram       = DRAM[Int](length)

    setMem(dram1, data1)
    setMem(dram2, data2)

    val out_fifofull = ArgOut[Int]
    val out_fifoempty = ArgOut[Int]
    val out_store_loc = ArgOut[Int]
    val out_fifofullafterstore = ArgOut[Int]

    Accel {
      val fifo     = FIFO[Int](tileSize)
      val sram1    = SRAM[Int](tileSize)
      val sram2    = SRAM[Int](tileSize)

      val stores = Reg[Int](0)

      Sequential.Foreach (0 until length by tileSize) { i=>

        Parallel {
          sram1 load dram1(i::i+tileSize)
          sram2 load dram2(i::i+tileSize)
        }

        Pipe.Foreach (0 until tileSize) { j=>
          Sequential{
            if (fifo.isFull) {
              outram(stores.value::stores.value+tileSize) store fifo
              stores := stores.value + tileSize
            }
          }

          val fromsram1 = sram1(j)
          val fromsram2 = sram2(j)

          if (fromsram2 == 1) {
            fifo.enq(fromsram1)
          }
        }
      }

      out_fifofull := mux(fifo.isFull, 1,0)
      out_fifoempty := mux(fifo.isEmpty, 1,0)
      out_store_loc := stores.value

      if (!fifo.isEmpty) {
        outram(stores.value::stores.value+fifo.numel) store fifo
        out_fifofullafterstore := mux(fifo.isFull,1,0)
      }
    }

    val golden = data1.zip(data2){(x,y) => pack(x,y) }.filter{t => t._2 == 1 }.map(_._1)

    val result = getMem(outram)

    for (i <- 0 until golden.length) {
      assert(result(i) == golden(i), "Mismatch " + i + ": " + result(i) + " != " + golden(i))
    }

    printArray(result, "Result : ")
    printArray(golden, "Gold : ")
    println("fifofull:"  + getArg(out_fifofull))
    println("fifoempty:"  + getArg(out_fifoempty))
    println("store_loc:"  + getArg(out_store_loc))
    println("fifofullafterstore:"  + getArg(out_fifofullafterstore))
  }
}


// Working
@test class Scan_fillfifo extends SpatialTest {
  override def runtimeArgs: Args = "128"


  def main(args: Array[String]): Unit = {
    val tileSize = 64
    val N = args(0).to[Int]

    val length = ArgIn[Int]
    setArg(length, N)

    val data1        = Array.tabulate(N){i => i * 3}
    val data2        = Array.tabulate(N){i => i % 2}

    val dram1        = DRAM[Int](length)
    val dram2        = DRAM[Int](length)
    val outram       = DRAM[Int](length)

    setMem(dram1, data1)
    setMem(dram2, data2)

    Accel {
      val fifo   = FIFO[Int](tileSize)
      val sram1  = SRAM[Int](tileSize)
      val sram2  = SRAM[Int](tileSize)
      val memptr = Reg[Int](0)

      Sequential.Foreach (0 until length by tileSize) { i=>

        Parallel {
          sram1 load dram1(i::i+tileSize)
          sram2 load dram2(i::i+tileSize)
        }

        Pipe.Foreach (0 until tileSize) { j=>
          val fromsram1 = sram1(j)
          val fromsram2 = sram2(j)

          if (fromsram2 == 1) {
            fifo.enq(fromsram1)
          }
        }

        // Fill remainder with 0s
        //          FSM(0)(filler => filler != 1){filler =>
        //            if (!fifo.full()) {
        //                Pipe{fifo.enq(-1)}
        //            }
        //          }{ filler => mux(fifo.full(), 1, 0)}

        val words = fifo.numel
        outram(memptr::memptr+words) store fifo
        memptr := memptr.value + words
      }
    }

    val golden = data1.zip(data2){(x,y) => pack(x,y) }.filter{t => t._2 == 1 }.map(_._1)
    val result = getMem(outram)
    printArray(result, "Result : ")

    for (i <- 0 until golden.length) {
      assert(result(i) == golden(i), "Mismatch " + i + ": " + result(i) + " != " + golden(i))
    }
  }
}

@test class Scan_filter extends SpatialTest {
  override def runtimeArgs: Args = "128"


  def main(args: Array[String]): Unit = {
    val tileSize = define("tileSize", 64)
    val parN = define("par", 1)
    val N = args(0).to[Int]
    val length = ArgIn[Int]

    Console.out.println(s"tileSize: $tileSize, par: $parN")
    setArg(length, N)

    val data        = Array.tabulate(N){i => i%16 }
    val dram        = DRAM[Int](length)
    val outram      = DRAM[Int](length)
    setMem(dram, data)

    Accel {

      'Scan.Pipe.Foreach (0 until length by tileSize par parN) { i=>
        val sramX    = SRAM[Int](tileSize)
        val sramY    = SRAM[Int](tileSize)
        sramX load dram(i::i+tileSize)
        filter(sramY,{e:Int=>(e>=8)},sramX)
        outram(i::i+tileSize) store sramY
      }
    }
    val golden = Array.tabulate(N){i => if(data(i) >= 8) 1.to[Int] else 0.to[Int]}
    val result = getMem(outram)
    printArray(getMem(dram), " Input : ")
    printArray(getMem(outram), "Output : ")
    printArray(golden, "Golden : ")

    for (i <- 0 until golden.length){
      assert(golden(i) == result(i), "Mismatch " + i + ": " + result(i) + " != " + golden(i))
    }
  }
}

@test class Scan_filter_wait extends SpatialTest {
  override def runtimeArgs: Args = "128 16"


  def main(args: Array[String]): Unit = {
    val tileSize = define("tileSize", 64)
    val parN = define("par", 1)
    val N = args(0).to[Int]
    val K = args(1).to[Int]
    val length = ArgIn[Int]
    val wait = ArgIn[Int]
    Console.out.println(s"tileSize: $tileSize, par: $parN")
    setArg(length, N)
    setArg(wait, K)

    val data        = Array.tabulate(N){i => i%16 }
    val dram        = DRAM[Int](length)
    val outram      = DRAM[Int](length)
    setMem(dram, data)

    Accel {
      'Scan.Pipe.Foreach (0 until length by tileSize par parN) { i=>
        val sramX = SRAM[Int](tileSize)
        val sramY = SRAM[Int](tileSize)
        sramX load dram(i::i+tileSize)
        filter(sramY,
          {e: Int=>
            val tmp = Reg[Int]
            'Wait.Foreach (0 until wait) { i => tmp := i+1}
            (e>=8)
          },
          sramX)
        outram(i::i+tileSize) store sramY
      }
    }
    val golden = Array.tabulate(N){i => if(data(i) >= 8) 1.to[Int] else 0.to[Int]}
    val result = getMem(outram)
    printArray(getMem(dram), " Input : ")
    printArray(result, "Output : ")
    printArray(golden, "Golden : ")

    for (i <- 0 until golden.length){
      assert(golden(i) == result(i), "Mismatch " + i + ": " + result(i) + " != " + golden(i))
    }
  }
}



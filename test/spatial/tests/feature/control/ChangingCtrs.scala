package spatial.tests.feature.control

import spatial.dsl._

@spatial class ChangingCtrMax extends SpatialTest {
  val tileSize = 16
  val N = 5

  def changingctrmax[T:Num](): Array[T] = {
    val result = DRAM[T](16)
    Accel {
      val rMem = SRAM[T](16)
      Sequential.Foreach(16 by 1) { i =>
        val accum = Reduce(0)(i by 1){ j => j }{_+_}
        rMem(i) = accum.value.to[T]
      }
      result(0::16 par 16) store rMem
    }
    getMem(result)
  }


  def main(args: Array[String]): Unit = {

    val result = changingctrmax[Int]()

    // Use strange if (i==0) b/c iter1: 0 by 1 and iter2: 1 by 1 both reduce to 0
    val gold = Array.tabulate(tileSize) { i => if (i==0) 0 else (i-1)*i/2}

    printArray(gold, "gold: ")
    printArray(result, "result: ")
    assert(result == gold)
  }
}


@spatial class ChangingCtrMax2 extends SpatialTest {
  type T = FixPt[TRUE,_16,_16]

  // Inspired by FFT_Strided
  def main(args: Array[String]): Unit = {

    val FFT_SIZE = 256
    val numiter = (scala.math.log(FFT_SIZE) / scala.math.log(2)).to[Int] // = 8

    val result_real_dram = DRAM[T](FFT_SIZE)

    Accel{
      val data_real_sram = SRAM[T](FFT_SIZE)
      Foreach(FFT_SIZE by 1){ i => data_real_sram(i) = 0 }
      val span = Reg[Int](FFT_SIZE)
      'NUMITER.Foreach(0 until numiter) { log => 
        span := span >> 1
        val num_sections = Reduce(Reg[Int](1))(0 until log){i => 2}{_*_}
        'NUM_SECTION.Foreach(0 until num_sections) { section => 
          'SPAN.Sequential.Foreach(0 until span by 1) { offset => 
            data_real_sram(section) = data_real_sram(section) + 1
          }
        }
      }
      result_real_dram store data_real_sram
    }

    val gold = Array[T](255,
                           127,
                               63,63,
                                     31,31,31,31,
                                                 15,15,15,15,15,15,15,15,
                                                                         7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
                        3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,
                        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
                        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
                        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
                        0,0,0,0,0)

    val result_real = getMem(result_real_dram)


    printArray(result_real, "Result real: ")
    printArray(gold, "gold: ")
    println(r"Pass: ${gold == result_real}")
    assert(gold == result_real)

  }
}

@spatial class ChangingCtrBounds extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val d = DRAM[Int](16,16)
    val bounds = DRAM[Int](16,2)
    val boundsData = (0::16,0::2){(i,j) => 
      if (j == 0) i % 4 + 3
      else i % 4 + 6
    }
    setMem(bounds, boundsData)

    Accel{
      val s = SRAM[Int](16,16)
      Foreach(16 by 1, 16 by 1){(i,j) => s(i,j) = 0}
      val bnds = SRAM[Int](16,2)
      bnds load bounds
      Foreach(16 by 1){row => 
        val lower = bnds(row, 0)
        val upper = bnds(row, 1)
        // Glitch occurs when lower of last iter equals upper of this iter
        Foreach(lower until upper by 1){j => 
          val reg = Reg[Int](0)
          Pipe{reg := j}
          Pipe{s(row,j) = reg.value}
        }
      }
      d store s
    }

    val gold = (0::16, 0::16){(i,j) => 
      val lower = boundsData(i,0)
      val upper = boundsData(i,1)
      if (j >= lower && j < upper) j
      else 0
    }

    val result = getMatrix(d)
    printMatrix(gold, "gold: ")
    printMatrix(result, "result: ")
    assert(result == gold)
  }
}

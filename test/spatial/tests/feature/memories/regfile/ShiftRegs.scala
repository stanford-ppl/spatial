package spatial.tests.feature.memories.regfile

import spatial.dsl._

@spatial class ShiftRegs extends SpatialTest {
  override def runtimeArgs: Args = "32"

  def main(args: Array[String]): Unit = {
    val init_dram = DRAM[I32](3,3)
    val regfile_dram = DRAM[I32](3,3)
    val parshiftregfile_dram = DRAM[I32](3,3)
    val shiftregfile_dram = DRAM[I32](3,3)
    val triplebuf_dram = DRAM[I32](8,8)
    val dummy = ArgOut[I32]

    Accel {
      val init_reg = RegFile[I32](3,3,List.tabulate[I32](9){i => i})
      init_dram store init_reg

      Foreach(3 by 1, 3 by 1){(i,j) => print(init_reg(i,j) + " ") }
      println("\n")

      val regfile = RegFile[I32](3,3)
      Foreach(3 by 1, 3 by 1 par 3){(i,j) => regfile(i,j) = i+j}
      regfile_dram store regfile

      val parshiftregfile = RegFile[I32](3,3)
      Foreach(3 by 1){j => 
      	Foreach(3 by 1 par 3){i => parshiftregfile(i,*) <<= i+1+j}
      }
      parshiftregfile_dram store parshiftregfile

      val shiftregfile = RegFile[I32](3,3)
      Foreach(3 by 1){j => 
      	Foreach(3 by 1){i => shiftregfile(i,*) <<= i+1+j}
      }
      shiftregfile_dram store shiftregfile

      val triplebuf = RegFile[I32](8,Seq.tabulate[I32](8){i => i})
      Foreach(8 by 1){i => 
        triplebuf_dram(i, 0::8) store triplebuf
        Pipe{dummy := i}
        Foreach(8 by 1){j => triplebuf(j) = j + i + 1 }
      }
    }

    println(r"ignore this: $dummy")
    val init = getMem(init_dram)
    println(r"Expect A(i) = i, got: ")
    printArray(init)
    assert(init == Array.tabulate(9){i => i})

    val regfile_result = getMatrix(regfile_dram)
    val parshiftregfile_result = getMatrix(parshiftregfile_dram)
    val shiftregfile_result = getMatrix(shiftregfile_dram)
    val triplebuf_result = getMatrix(triplebuf_dram)

    println("GOT                 WANTED")
    for (i <- 0 until 3){
      for (j <- 0 until 3){print(r"${regfile_result(i,j)} ")}
      print("     ")
      for (j <- 0 until 3){print(r"${i+j} ")}
      println("")
    }
    println("")
    for (i <- 0 until 3){
      for (j <- 0 until 3){print(r"${shiftregfile_result(i,j)} ")}
      print("     ")
      for (j <- 0 until 3){print(r"${i+1+(2-j)} ")}
      println("")
    }
    println("")
    for (i <- 0 until 3){
      for (j <- 0 until 3){print(r"${parshiftregfile_result(i,j)} ")}
      print("     ")
      for (j <- 0 until 3){print(r"${i+1+(2-j)} ")}
      println("")
    }
    for (i <- 0 until 5){
      for (j <- 0 until 8){print(r"${triplebuf_result(i,j)} ")}
      print("     ")
      for (j <- 0 until 8){print(r"${if (i <= 2) j else i-1+j} ")}
      println("")
    }

    for (i <- 0 until 3){
      for (j <- 0 until 3){
    		assert(regfile_result(i,j) == i+j)
    		assert(shiftregfile_result(i,j) == i+1+(2-j))
    		assert(parshiftregfile_result(i,j) == i+1+(2-j))
        assert(triplebuf_result(i,j) == {if (i <= 2) j else i-1+j})
    	}
    }

  }
}

@spatial class CrossRead extends SpatialTest {
  /** This app tests parallel reads (one full row, one full column) to a RegFile, 
    * where one of the elements is shared between the col and the row
    */

  def main(args: Array[String]): Unit = {
    val dim = 8
    val init_dram = DRAM[I32](dim,dim)
    val init_data = (0::dim,0::dim){(i,j) => i*j}
    setMem(init_dram, init_data)
    val result = ArgOut[I32]

    Accel {
      val init_reg = RegFile[I32](dim,dim)
      Foreach(dim by 1){j => 
        List.tabulate(dim){i => init_reg(i,*) <<= i*j} // Shifts into col 0
        val rowSum = List.tabulate(dim){i => init_reg(2, i)}.reduceTree{_+_}
        val colSum = List.tabulate(dim){i => init_reg(i, 2)}.reduceTree{_+_}
        result := rowSum + colSum
      }
    }

    val got = getArg(result)
    val gold = List.tabulate(dim){i => init_data(5, i)}.reduce{_+_} + List.tabulate(dim){i => init_data(i, 2)}.reduce{_+_}
    println(r"Got $got, expected $gold")
    println(r"Pass: ${got == gold}")
    assert(got == gold)

  }
}
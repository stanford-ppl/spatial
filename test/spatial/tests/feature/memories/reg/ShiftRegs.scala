package spatial.tests.feature.memories.reg

import spatial.dsl._

@spatial class ShiftRegs extends SpatialTest {
  override def runtimeArgs: Args = "32"

  def main(args: Array[String]): Unit = {
    val init_dram = DRAM[I32](3,3)
    val regfile_dram = DRAM[I32](3,3)
    val parshiftregfile_dram = DRAM[I32](3,3)
    val shiftregfile_dram = DRAM[I32](3,3)

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

    }

    val init = getMem(init_dram)
    println(r"Expect A(i) = i, got: ")
    printArray(init)
    assert(init == Array.tabulate(9){i => i})

    val regfile_result = getMatrix(regfile_dram)
    val parshiftregfile_result = getMatrix(parshiftregfile_dram)
    val shiftregfile_result = getMatrix(shiftregfile_dram)

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

    for (i <- 0 until 3){
      for (j <- 0 until 3){
    		assert(regfile_result(i,j) == i+j)
    		assert(shiftregfile_result(i,j) == i+1+(2-j))
    		assert(parshiftregfile_result(i,j) == i+1+(2-j))
    	}
    }

  }
}
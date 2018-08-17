import spatial.dsl._

@spatial object HelloSpatial extends SpatialApp {

  def main(args: Array[String]): Unit = {
    val argin1 = ArgIn[Int]   // Register that is written to by the host and read from by the Accel
    val argout1 = ArgOut[Int] // Register that is written to by the Accel and read from by the host
    val io1 = HostIO[Int]     // Register that can be both written to and read from by the Accel and the host

    type T = FixPt[FALSE, _16, _16] // 32-bit unsigned integer with 16 whole bits and 16 fractional bits.
    type Flt = Float // 32-bit standard Float

    val argin2 = ArgIn[T]

    setArg(argin1, args(0).to[Int]) // Set argument with the first command-line value
    setArg(argin2, 7.to[T]) // Args do not necessarily need to be set with command-line values
    setArg(io1, args(1).to[Int])

    val data1D        = Array.tabulate(64){i => i * 3} // Create 1D array with 64 elements, each element being index * 3
    val data1D_longer = Array.tabulate(1024){i => i} // Create 1D array with 1024 elements
    val data2D        = (0::64, 0::64){(i,j) => i*100 + j} // Create 64x64 2D, where each element is row * 100 + col
    val data5D        = (0::2, 0::2, 0::2, 0::2, 0::16){(i,j,k,l,m) => random[Int](5)} // Create 5D tensor, the highest dimension tensor currently supported in Spatial, with each element a random Int between 0 and 5

    val dram1D        = DRAM[Int](64)
    val dram1D_longer = DRAM[Int](1024)
    val dram2D        = DRAM[Int](64,64)
    val dram5D        = DRAM[Int](2,2,2,2,16)

    setMem(dram1D, data1D)
    setMem(dram1D_longer, data1D_longer)
    setMem(dram2D, data2D)
    setMem(dram5D, data5D)

    val dram_result2D = DRAM[Int](32,32)
    val dram_scatter1D = DRAM[Int](1024)

    Accel {
      val sram1D        = SRAM[Int](64)
      val sram2D        = SRAM[Int](32,32)
      val sram5D        = SRAM[Int](2,2,2,2,16)

      sram1D load dram1D // Load data from a DRAM of matching dimension
      sram2D load dram2D(32::64, 0::32 par 16) // Load region from DRAM. In this case, we load the bottom-left quadrant of data from dram2D
      sram5D load dram5D // Load 5D tensor

      dram_result2D(0::32, 0::32 par 8) store sram2D

      val gathered_sram = SRAM[Int](64)  // Create SRAM to hold data
      gathered_sram gather dram1D_longer(sram1D par 1, 64)  // Use the first 64 elements in sram1D as the addresses in dram1D_longer to collect, and store them into gathered_sram

      dram_scatter1D(sram1D par 1, 64) scatter gathered_sram // For the first 64 elements, place element i of gathered_sram into the address indicated by the i-th element of sram1D

      val reg1 = Reg[Int](5) // Create register with initial value of 5
      val reg2 = Reg[T] // Default initial value for a Reg is 0
      Pipe{reg1 := argin1} // Load from ArgIn
      Pipe{reg2 := argin2} // Load from ArgIn
      argout1 := reg1 + reg2.value.to[Int] // Cast the value in reg2 to Int and add it to reg1
      io1 := reg1
    }

    val result_scattered = getMem(dram_scatter1D)
    val result2D = getMatrix(dram_result2D) // Collect 2D dram as a "Matrix."  Likewise, 3, 4, and 5D regions use "getTensor3D", "getTensor4D", and "getTensor5D"

    printMatrix(result2D, "Result 2D: ")
    printArray(result_scattered, "Result Scattered: ")
    val gold_2D = (32::64, 0::32){(i,j) => i*100 + j} // Remember we took bottom-left corner
    val cksum_2D = gold_2D.zip(result2D){_==_}.reduce{_&&_} // Zip the gold with the result and check if they are all equal
    val cksum_scattered = Array.tabulate(64){i => result_scattered(3*i) == 3*i}.reduce{_&&_} // Check if every 3 entries is equal to the index
    println("2D pass? " + cksum_2D)
    println("scatter pass? " + cksum_scattered)


    val result1 = getArg(argout1)
    val result2 = getArg(io1)

    println("Received " + result1 + " and " + result2)
    val cksum = (result1 == {args(0).to[Int] + args(1).to[Int]}) && (result2 == args(0).to[Int]) // The {} brackets are Scala's way of scoping operations
    println("ArgTest pass? " + cksum)
  }
}



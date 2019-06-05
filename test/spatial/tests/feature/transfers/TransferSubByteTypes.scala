import spatial.dsl._

@spatial class TransferSubByteTypes extends SpatialTest { 
  def main(args: Array[String]): Void = {
    val startInt = 16
    val endInt = 1032
    val size_b = endInt - startInt
    val start = ArgIn[Int]
    val end = ArgIn[Int]
    setArg(start, startInt)
    setArg(end, endInt)

    val bits_DRAM = DRAM[Bit](size_b)
    val stored_bits_DRAM = DRAM[Bit](size_b)
    val bits_data = Array.tabulate(size_b) { i => if (i % 5 == 0) true else false }
    setMem(bits_DRAM, bits_data)
    setMem(stored_bits_DRAM, Array.fill[Bit](size_b)(false))

    val b2_DRAM = DRAM[UInt2](size_b/2)
    val stored_b2_DRAM = DRAM[UInt2](size_b/2)
    val b2_data = Array.tabulate(size_b/2) { i => (i % 3).to[UInt2] }
    setMem(b2_DRAM, b2_data)
    setMem(stored_b2_DRAM, Array.fill[UInt2](size_b/2)(0.to[UInt2]))

    val b4_DRAM = DRAM[UInt4]((size_b/4))
    val stored_b4_DRAM = DRAM[UInt4]((size_b/4))
    val b4_data = Array.tabulate((size_b/4)) { i => (i % 5).to[UInt4] }
    setMem(b4_DRAM, b4_data)
    setMem(stored_b4_DRAM, Array.fill[UInt4](size_b/4)(0.to[UInt4]))

    Accel {
       val bits_SRAM = SRAM[Bit](size_b)
       bits_SRAM load bits_DRAM(start::end par 8)
       stored_bits_DRAM(start::end par 8) store bits_SRAM

       val b2_SRAM = SRAM[UInt2](size_b/2)
       b2_SRAM load b2_DRAM(start/2::end/2 par 4)
       stored_b2_DRAM(start/2::end/2 par 4) store b2_SRAM

       val b4_SRAM = SRAM[UInt4]((size_b/4))
       b4_SRAM load b4_DRAM(start/4::end/4 par 2)
       stored_b4_DRAM(start/4::end/4 par 2) store b4_SRAM
    }

    val all_bits = getMem(stored_bits_DRAM)
    val all_bits_gold = Array.tabulate(size_b){i => if (i < startInt) 0 else bits_data(i)}
    printArray(all_bits, "Got bits")
    printArray(all_bits_gold, "Wanted bits")

    val all_b2 = getMem(stored_b2_DRAM)
    val all_b2_gold = Array.tabulate(size_b/2){i => if (i < (startInt/2)) 0 else b2_data(i)}
    printArray(all_b2, "Got b2")
    printArray(all_b2_gold, "Wanted b2")

    val all_b4 = getMem(stored_b4_DRAM)
    val all_b4_gold = Array.tabulate(size_b/4){i => if (i < (startInt/4)) 0 else b4_data(i)}
    printArray(all_b4, "Got b4")
    printArray(all_b4_gold, "Wanted b4")

    println(r"bit: ${all_bits == all_bits_gold}")
    println(r"uint2: ${all_b2 == all_b2_gold}")
    println(r"b4: ${all_b4 == all_b4_gold}")
    assert(all_b2 == all_b2_gold && all_bits == all_bits_gold && all_b4 == all_b4_gold)

  }
}  
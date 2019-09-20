import spatial.dsl._


@spatial class Sort_1 extends SpatialTest {
 /*                                                                                                  
    TODO: Cartoon of what this is doing                                                         
                                                                                                                                                                                                                       
                                                                                                                                                                           
 */

  override def dseModelArgs: Args = "16 50 50 50 50 50"
  override def finalModelArgs: Args = "16 50 50 50 50 50"

  def main(args: Array[String]): Unit = {

    val numel = 2048
    val NUM_BLOCKS = 512
    val EL_PER_BLOCK = 4
    val RADIX = 4
    val BUCKET_SIZE = NUM_BLOCKS*RADIX
    val SCAN_BLOCK = 16
    val SCAN_RADIX = BUCKET_SIZE/SCAN_BLOCK
    val a = false.to[Bit]
    val b = true.to[Bit]

    val raw_data = loadCSV1D[Int](s"$DATA/sort/sort_data.csv", "\n")

    val data_dram = DRAM[Int](numel)

    setMem(data_dram, raw_data)

    Accel{
      val a_sram = SRAM[Int](numel)
      val b_sram = SRAM[Int](numel)
      val bucket_sram = SRAM[Int](BUCKET_SIZE)
      val sum_sram = SRAM[Int](SCAN_RADIX)
      val valid_buffer = Reg[Bit](false)
      
      a_sram load data_dram

      def hist(exp: I32, s: SRAM1[Int]): Unit = {
        Foreach(NUM_BLOCKS by 1) { blockID => 
          Sequential.Foreach(4 by 1) {i => 
            val a_indx = blockID * EL_PER_BLOCK + i
            // val a_indx = Reg[Int](0)
            // a_indx := blockID * EL_PER_BLOCK + i
            val shifted = Reg[Int](0)
            shifted := s(a_indx) // TODO: Allow just s(a_indx) >> exp syntax
            // Reduce(shifted)(exp by 1) { k => shifted >> 1}{(a,b) => b}
            Foreach(exp by 1) { k => shifted := shifted.value >> 1}
            val bucket_indx = (shifted.value & 0x03)*NUM_BLOCKS + blockID + 1
            // println(" hist bucket(" + bucket_indx + ") = " + bucket_sram(bucket_indx) + " + 1")
            // println(r"shifted started with ${s(a_indx)} (from ${a_indx}), now is ${shifted.value}, + $blockID")
            if (bucket_indx < 2048) {bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1}
          }
        }
      }

      def local_scan(): Unit = {
        Foreach(SCAN_RADIX by 1) { radixID => 
          Sequential.Foreach(1 until SCAN_BLOCK by 1) { i => // Loop carry dependency
            val bucket_indx = radixID*SCAN_BLOCK.to[Int] + i
            val prev_val = Reg[Int](0)
            Pipe{ prev_val := bucket_sram(bucket_indx - 1) }
            Pipe{ bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + prev_val }
            // println(r"local_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${prev_val}")
          }
        }
      }

      def sum_scan(): Unit = {
        sum_sram(0) = 0
        Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => // Remove manual II when bug #207 (or #151?) is fixed
        // Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => 
          val bucket_indx = radixID*SCAN_BLOCK - 1
          sum_sram(radixID) = sum_sram(radixID-1) + bucket_sram(bucket_indx)
          // println(r"sum_scan: sum_sram(${radixID}) = ${sum_sram(radixID-1)} + ${bucket_sram(bucket_indx)}")
        }
      }

      def last_step_scan(): Unit = {
        Foreach(SCAN_RADIX by 1) { radixID => 
          Foreach(SCAN_BLOCK by 1) { i => 
            val bucket_indx = radixID * SCAN_BLOCK + i
            bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + sum_sram(radixID)
            // println(r"last_step_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${sum_sram(radixID)}")
          }
        }
      }

      def update(exp: I32, s1: SRAM1[Int], s2: SRAM1[Int]): Unit = {

        Foreach(NUM_BLOCKS by 1) { blockID => 
          Sequential.Foreach(4 by 1) { i => 
            val shifted = Reg[Int](0)
            shifted := s1(blockID*EL_PER_BLOCK + i) // TODO: Allow just s(a_indx) >> exp syntax
            // Reduce(shifted)(exp by 1) { k => shifted >> 1}{(a,b) => b}
            Foreach(exp by 1) { k => shifted := shifted >> 1}
            val bucket_indx = (shifted & 0x3)*NUM_BLOCKS + blockID
            val a_indx = blockID * EL_PER_BLOCK + i
            s2(bucket_sram(bucket_indx)) = s1(a_indx)
            bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1
            // println(r"update: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + 1")
          }
        }
      }


      Sequential.Foreach(32 by 2) { exp => 
        Foreach(BUCKET_SIZE by 1 par 16) { i => bucket_sram(i) = 0 }
  
        if (valid_buffer == a) {
          Pipe{hist(exp, a_sram)}
        } else {
          Pipe{hist(exp, b_sram)}
        }

        local_scan()
        sum_scan()
        last_step_scan()

        if (valid_buffer == a) {
          // println("s1 = a, s2 = b")
          Sequential{
            Pipe{update(exp, a_sram, b_sram)}
            Pipe{valid_buffer := b}
          }
        } else {
          // println("s1 = b, s2 = a")
          Sequential{
            Pipe{update(exp, b_sram, a_sram)}
            Pipe{valid_buffer := a}
          }
        }

      }

      if (valid_buffer == a) {
        // println("dumping buf a")
        data_dram store a_sram
      } else {
        // println("dumping buf b")
        data_dram store b_sram
      }

    }
    val sorted_gold = loadCSV1D[Int](s"$DATA/sort/sort_gold.csv", "\n")
    val sorted_result = getMem(data_dram)

    printArray(sorted_gold, "Sorted Gold: ")
    printArray(sorted_result, "Sorted Result: ")

    val cksum = sorted_gold.zip(sorted_result){_==_}.reduce{_&&_}
    printArray(sorted_gold.zip(sorted_result){_==_}, "Matchup: ")
    // // Use the real way to check if list is sorted instead of using machsuite gold
    // // This way says I've done goofed, issue #
    // val cksum = Array.tabulate(STOP-1){ i => pack(sorted_result(i), sorted_result(i+1)) }.map{a => a._1 <= a._2}.reduce{_&&_}
    println("PASS: " + cksum + " (Sort_Radix)")
    assert(cksum)
  }
}


@spatial class Sort_2 extends SpatialTest {  // Replace shift loop
 /*                                                                                                  
    TODO: Cartoon of what this is doing                                                         
                                                                                                                                                                                                                       
                                                                                                                                                                           
 */

  override def dseModelArgs: Args = "16 50 50 50 50 50"
  override def finalModelArgs: Args = "16 50 50 50 50 50"

  def main(args: Array[String]): Unit = {

    val numel = 2048
    val NUM_BLOCKS = 512
    val EL_PER_BLOCK = 4
    val RADIX = 4
    val BUCKET_SIZE = NUM_BLOCKS*RADIX
    val SCAN_BLOCK = 16
    val SCAN_RADIX = BUCKET_SIZE/SCAN_BLOCK
    val a = false.to[Bit]
    val b = true.to[Bit]

    val raw_data = loadCSV1D[Int](s"$DATA/sort/sort_data.csv", "\n")

    val data_dram = DRAM[Int](numel)

    setMem(data_dram, raw_data)

    Accel{
      val a_sram = SRAM[Int](numel)
      val b_sram = SRAM[Int](numel)
      val bucket_sram = SRAM[Int](BUCKET_SIZE)
      val sum_sram = SRAM[Int](SCAN_RADIX)
      val valid_buffer = Reg[Bit](false)
      
      a_sram load data_dram

      def hist(exp: I32, s: SRAM1[Int]): Unit = {
        Foreach(NUM_BLOCKS by 1, 4 by 1) { (blockID, i) => 
          val a_indx = blockID * EL_PER_BLOCK + i
          // val a_indx = Reg[Int](0)
          // a_indx := blockID * EL_PER_BLOCK + i

          val unshifted = s(a_indx)
          val shiftMatch = List.tabulate(16){i => exp == (2*i)}
          val shiftVals = List.tabulate(16){i => unshifted >> (2*i)}
          val shifted =  oneHotMux(shiftMatch, shiftVals)

          val bucket_indx = (shifted & 0x03)*NUM_BLOCKS + blockID + 1
          // println(" hist bucket(" + bucket_indx + ") = " + bucket_sram(bucket_indx) + " + 1")
          // println(r"shifted started with ${s(a_indx)} (from ${a_indx}), now is ${shifted.value}, + $blockID")
          if (bucket_indx < 2048) {bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1}
        }
      }

      def local_scan(): Unit = {
        Foreach(SCAN_RADIX by 1, 1 until SCAN_BLOCK) { (radixID, i) => 
          val bucket_indx = radixID*SCAN_BLOCK.to[Int] + i
          val prev_val = Reg[Int](0)
          prev_val := bucket_sram(bucket_indx - 1)
          bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + prev_val
          // println(r"local_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${prev_val}")
        }
      }

      def sum_scan(): Unit = {
        sum_sram(0) = 0
        Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => // Remove manual II when bug #207 (or #151?) is fixed
        // Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => 
          val bucket_indx = radixID*SCAN_BLOCK - 1
          sum_sram(radixID) = sum_sram(radixID-1) + bucket_sram(bucket_indx)
          // println(r"sum_scan: sum_sram(${radixID}) = ${sum_sram(radixID-1)} + ${bucket_sram(bucket_indx)}")
        }
      }

      def last_step_scan(): Unit = {
        Foreach(SCAN_RADIX by 1, SCAN_BLOCK by 1) { (radixID, i) => 
          val bucket_indx = radixID * SCAN_BLOCK + i
          bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + sum_sram(radixID)
          // println(r"last_step_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${sum_sram(radixID)}")
        }
      }

      def update(exp: I32, s1: SRAM1[Int], s2: SRAM1[Int]): Unit = {

        Pipe.II(2).Foreach(NUM_BLOCKS by 1, 4 by 1) { (blockID, i) => 
        // Foreach(NUM_BLOCKS by 1, 4 by 1) { (blockID, i) => 

          val unshifted = s1(blockID*EL_PER_BLOCK + i)
          val shiftMatch = List.tabulate(16){i => exp == (2*i)}
          val shiftVals = List.tabulate(16){i => unshifted >> (2*i)}
          val shifted =  oneHotMux(shiftMatch, shiftVals)

          val bucket_indx = (shifted & 0x3)*NUM_BLOCKS + blockID
          val a_indx = blockID * EL_PER_BLOCK + i
		  s2(bucket_sram(bucket_indx)) = s1(a_indx)
		  bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1
          // println(r"update: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + 1")
        }
      }


      Sequential.Foreach(32 by 2) { exp => 
        Foreach(BUCKET_SIZE by 1 par 16) { i => bucket_sram(i) = 0 }
  
        if (valid_buffer == a) {
          hist(exp, a_sram)
        } else {
          hist(exp, b_sram)
        }

        local_scan()
        sum_scan()
        last_step_scan()

        if (valid_buffer == a) {
          // println("s1 = a, s2 = b")
            update(exp, a_sram, b_sram)
            valid_buffer := b
        } else {
          // println("s1 = b, s2 = a")
            update(exp, b_sram, a_sram)
            valid_buffer := a
        }


      }

      if (valid_buffer == a) {
        // println("dumping buf a")
        data_dram store a_sram
      } else {
        // println("dumping buf b")
        data_dram store b_sram
      }

    }

    val sorted_gold = loadCSV1D[Int](s"$DATA/sort/sort_gold.csv", "\n")
    val sorted_result = getMem(data_dram)

    printArray(sorted_gold, "Sorted Gold: ")
    printArray(sorted_result, "Sorted Result: ")

    val cksum = sorted_gold.zip(sorted_result){_==_}.reduce{_&&_}
    printArray(sorted_gold.zip(sorted_result){_==_}, "Matchup: ")
    // // Use the real way to check if list is sorted instead of using machsuite gold
    // // This way says I've done goofed, issue #
    // val cksum = Array.tabulate(STOP-1){ i => pack(sorted_result(i), sorted_result(i+1)) }.map{a => a._1 <= a._2}.reduce{_&&_}
    println("PASS: " + cksum + " (Sort_Radix)")
    assert(cksum)
  }
}

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
      reg1 := argin1 // Load from ArgIn
      reg2 := argin2 // Load from ArgIn
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



@spatial object PageRankGrazelle extends SpatialApp {
  //override def runtimeArgs: Args = "50 0.125"

  type Elem = FixPt[TRUE, _16, _16] // Float
  //type X = FixPt[TRUE, _16, _16] // Float
  type X = FixPt[TRUE, _32, _32] // Float

  type P = Fix[TRUE, _64, _0] // Long

  def cceil(num: Double): Long = {
    val numm = num.to[Long]
    val nummm = numm.to[Double]
    if (num === nummm) numm
    else numm + 1
  }

  val margin = 0.01

  def gather_out_degrees(arr: Array[Long], edges:Array[Long]) = {

    assert (edges.length % 4 == 0)


    for (idx <- 0 until arr.length) {
      arr(idx) = 0
    }

    for (idx <- 0 until edges.length) {
      val edge:Long = edges(idx)
      val valid = edge >> 63

      if (valid != 0) {
        val src = (edge & 0x0000ffffffffffffl).to[Int]
        val orig = arr(src)
        arr(src) = orig + 1
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val dir = "/armadillo/users/ctimothy/fpga-grazelle/test-data/pagerank/"
    val mat = "vs/ash331-sm.csv"

    // I/O
    val f = openBinary(dir.concat(mat), false)

    val numbers = readBinary[Long](f)

    val n_edge_spaces = (numbers.length - 2)
    val edges_per_container = 4
    val n_containers =  (n_edge_spaces) / edges_per_container
    val n_nodes = numbers(0)
    val n_edges = numbers(1)

    val itersIN = 1

    val n_thread = 2
    val container_per_compute = 2
    val container_batch_size = 4 // In containers

    val simd = container_per_compute * edges_per_container


    val edges_only = Array.tabulate(n_edge_spaces){ i => numbers(i + 2) }
    val out_degrees = Array.empty[Long](n_nodes.to[Int])

    gather_out_degrees(out_degrees, edges_only)

    println("n_containers: " + n_containers)
    println("n_nodes: " + n_nodes)
    println("n_edges: " + n_edges)
    println("n_containers: " + n_containers + " from files container with " + n_edge_spaces + " longs")


    val OCnodes = DRAM[X](n_nodes.to[Int] * 2)
    val OCcontainers = DRAM[Long](n_edge_spaces)
    val OCout_degrees = DRAM[Long](n_nodes.to[Int])

    val probes = DRAM[Long](5)

    val nodesInit = Array.tabulate(n_nodes.to[Int] * 2) { i => 1.to[X] / n_nodes.to[X] }

     val dampIN = 0.85f.to[X]

    //val n_thread = 16
    //val simd_per_thread = 16

    val OCNN = ArgIn[Long]
    val OCNcontainer = ArgIn[Long]
    val iters = ArgIn[Int]
    val damp = ArgIn[X]
    setArg(damp, dampIN)
    val probe_out = ArgOut[Long]
    val probe_out2 = ArgOut[Long]

    //
    val ud_fringe = 0
    val ud_all = 1

    setArg(OCNN, n_nodes.to[Long])
    setArg(OCNcontainer, n_containers.to[Long] )
    setArg(iters, itersIN)

    setMem(OCnodes, nodesInit)
    setMem(OCcontainers, edges_only)
    setMem(OCout_degrees, out_degrees)

    Accel {

      //// Global data structure among threads
      //
      val n_container_gz:Long = OCNcontainer
      val noupdate = (1.to[X] - damp) / OCNN.value.to[X]

      val iter_num = Reg[Bit]
      iter_num := 1.to[Bit]

      val probe_vals = SRAM[Long](5)

      probe_vals(0) = -1
      probe_vals(1) = -1
      probe_vals(2) = -1
      probe_vals(3) = -1
      probe_vals(4) = -1
      //// Two stages:
      //// 1. each thread reads src and aggregating update lists
      //// 2. Aggregating update list and writing result to Mem

      Sequential.Foreach(iters by 1) { iter =>

         val from = mux(iter_num, 0, OCNN)
         val to = mux(iter_num, OCNN, 0)

        val dst_prev = Reg[Long](-1)
        val val_prev = Reg[X](0)

        Sequential.Foreach(OCNcontainer.value.to[Int] by container_batch_size * n_thread) { group_iter_i =>

          val group_iter = group_iter_i.to[Long]
          val last_iteration = (group_iter_i.to[Long] + container_batch_size * n_thread) >= OCNcontainer.value

          val thd_updates_dst = List.tabulate(n_thread) {i=> SRAM[Long](container_batch_size)}
          val thd_updates_val = List.tabulate(n_thread) {i=> SRAM[X](container_batch_size)}
          val n_thd_updates = List.tabulate(n_thread) { i=>  Reg[Int](0)}

          val thd_updates_dst_s = List.tabulate(n_thread) {  j => Reg[Long]}
          val thd_updates_val_s = List.tabulate(n_thread) { i => Reg[X] }
          val n_thd_updates_s = List.tabulate(n_thread) { i=>  Reg[Boolean](false)}

          val thd_updates_dst_w = List.tabulate(n_thread + 1) { j => FIFO[Long](1) }
          val thd_updates_val_w = List.tabulate(n_thread + 1) { j => FIFO[X](1) }
          val ud_start_full = List.tabulate(n_thread + 1) { j => Reg[Bit](1.to[Bit])}

          ud_start_full.foreach(i => i.reset())

          val combine_cur_dst = Reg[Long](-1)
          val combine_accum = Reg[X](0)

          combine_cur_dst.reset()
          combine_accum.reset()

          val final_writes_dst = FIFO[Long](n_thread)
          val final_writes_val = FIFO[X](n_thread)
          probe_out := -1
          probe_out2 := -1

          Sequential { List.tabulate(n_thread) { thread =>
              val my_cur_dst = Reg[Long](-1)
              val my_accum = Reg[X](0)
              val my_cur_full = Reg[Bit](1.to[Bit])

              val cyc_update_dst = SRAM[Long](container_per_compute)
              val cyc_update_val = SRAM[X](container_per_compute)
              val cyc_update_full = SRAM[Bit](container_per_compute)

              val local_edges_l = SRAM[Long](simd * container_per_compute)

              val my_n_thd_updates = n_thd_updates(thread)
              val my_n_thd_updates_s = n_thd_updates_s(thread)

              my_cur_dst.reset()
              my_accum.reset()
              my_cur_full.reset()
              my_n_thd_updates.reset()
              my_n_thd_updates_s.reset()

            // Process in batches
            // Each iteration with all threads running in parallel

              // Calculating starting edge
              val batch_container_begin = group_iter.to[Long] + (thread.to[Long] * container_batch_size.to[Long])
              val batch_container_end = min(batch_container_begin + container_batch_size, n_container_gz)
              val batch_edge_begin = batch_container_begin * edges_per_container
              val my_thread_num_container = max((batch_container_end - batch_container_begin), 0)


              if (thread == 0) {
                probe_vals(0) = batch_container_begin
                probe_vals(1) = batch_container_end
                probe_vals(2) = batch_container_begin + container_batch_size
                probe_vals(3) = n_container_gz
                probe_vals(4) = min(batch_container_begin + container_batch_size, n_container_gz)
              }

              println("1: " + batch_container_begin + " " + batch_container_end + " " + batch_edge_begin + " " + my_thread_num_container)
              Sequential.Foreach(my_thread_num_container.to[Int] by container_per_compute) { compute_container_begin_idx_i =>

                  val compute_container_begin_idx = compute_container_begin_idx_i.to[Long]

                  val compute_simd_begin = batch_edge_begin + compute_container_begin_idx * edges_per_container

                  val num_active_container = max(min(container_per_compute, my_thread_num_container - compute_container_begin_idx), 0)
                  val num_active_container_edges = num_active_container * edges_per_container

                  val compute_simd_end = compute_simd_begin + num_active_container_edges
                  print("2: " + compute_container_begin_idx + " " + compute_simd_begin + " " + num_active_container + " " + num_active_container_edges + " ")
                  println(compute_simd_end)

                  local_edges_l load OCcontainers(compute_simd_begin :: compute_simd_end)

                  Sequential.Foreach(container_per_compute by 1 par container_per_compute){ compute_iter =>

                    val container_active = ((compute_container_begin_idx + compute_iter.to[Long]) < my_thread_num_container)

                    val container_begin = (compute_iter * edges_per_container)

                    val my_simd_srcs = FIFO[Long](edges_per_container)
                    val my_simd_srcs_od = FIFO[Long](edges_per_container)

                    println("3: " + container_active + " " + container_begin + " ")

                    if (container_active) {

                      val my_simd_dst = (
                        ((local_edges_l(container_begin + 0) & 0x7fff000000000000l) >> 48) |
                        ((local_edges_l(container_begin + 1) & 0x7fff000000000000l) >> 33) |
                        ((local_edges_l(container_begin + 2) & 0x7fff000000000000l) >> 18) |
                        ((local_edges_l(container_begin + 3) & 0x0007000000000000l) >>  3))

                     //print("simd_dst: " + my_simd_dst + "   ")

                      val not_valids = List.tabulate(edges_per_container){ i=> Reg[Bit] }

                      Sequential.Foreach (edges_per_container by 1 par edges_per_container) { edge_iter =>
                        Pipe {
                        val valid = (local_edges_l(container_begin + edge_iter) & 0x8000000000000000l).bit(63)
                          if (valid) {
                            val src = (local_edges_l(container_begin + edge_iter) & 0x0000ffffffffffffl)
                            val src_offset= src + from
                            my_simd_srcs.enq(src_offset)
                            my_simd_srcs_od.enq(src)
                            //print("\t\t" + src)
                          }
                        not_valids.zipWithIndex.foreach{ case (f, idx) => { if (idx == edge_iter) f := !valid } }
                        }
                      }
                      //println()

                      cyc_update_full(compute_iter) = !(not_valids.map(_.value).reduceTree{_|_}) //Checked
                      //println("full: " + cyc_update_full(compute_iter))

                      val local_src_prop = FIFO[X](edges_per_container)
                      val local_src_out_degrees = FIFO[Long](edges_per_container)

                      local_src_prop gather OCnodes(my_simd_srcs)
                      local_src_out_degrees gather OCout_degrees(my_simd_srcs_od)


                       //print("val: ")
                      val partial_pr = Pipe.Reduce(Reg[X](0))(my_simd_srcs.numel by 1 par edges_per_container) { i =>
                        val a = local_src_prop.deq()
                        val b = local_src_out_degrees.deq().to[X]
                        //println(a + " " + b)
                         a / b  * damp
                        //local_src_prop.deq() / local_src_out_degrees.deq().to[X] * damp
                      }{_+_}
                      println("4: " + partial_pr)

                      cyc_update_dst(compute_iter) = my_simd_dst
                      cyc_update_val(compute_iter) = partial_pr.value
                    }
                  }

                  //Combining results and write update to local structure
                  Sequential.Foreach (num_active_container.to[Int] by 1) { c =>
                    val container_dst = cyc_update_dst(c)
                    val container_val = cyc_update_val(c)
                    val container_full = cyc_update_full(c)
                    //println("dst: " + container_dst + " val: " + container_val)


                    if (my_cur_dst != container_dst && my_cur_dst != -1) {
                      if (my_n_thd_updates == 0) {
                        //println("enquing thd: " + thread + " dst: " + my_cur_dst + " accum: " + my_accum + " s")
                        thd_updates_dst(thread)(my_n_thd_updates) = (my_cur_dst + to)
                        thd_updates_val(thread)(my_n_thd_updates) = (my_accum)
                        ud_start_full(thread) := my_cur_full
                      } else {
                        my_accum := my_accum + noupdate
                        thd_updates_dst(thread)(my_n_thd_updates) = (my_cur_dst + to)
                        thd_updates_val(thread)(my_n_thd_updates) = (my_accum)
                        ////If it is not the first and last, we can safely add first term
                        //println("enquing thd: " + thread + " dst: " + my_cur_dst + " accum: " + my_accum + " + " + noupdate + " = " + my_accum)
                        //println("n: " + thread + " dst: " + my_cur_dst + " accum: "  + my_accum)
                      }
                      my_n_thd_updates := my_n_thd_updates + 1
                      my_accum := container_val
                    } else {
                      my_accum := my_accum + container_val
                    }
                    my_cur_dst := container_dst
                    my_cur_full := container_full
                  }
                }

                // Completing fringe
                if (my_cur_full) {
                  //println("enquing thd: " + thread + " dst: " + my_cur_dst + " accum: " + my_accum + " f")
                  thd_updates_dst_s(thread) := (my_cur_dst.value + to)
                  thd_updates_val_s(thread) := (my_accum.value)
                  my_n_thd_updates_s := true
                } else {
                  //println("f: " + thread + " dst: " + my_cur_dst + " accum: "  + (my_accum.value + noupdate))
                  thd_updates_dst(thread)(my_n_thd_updates) = (my_cur_dst.value + to)
                  thd_updates_val(thread)(my_n_thd_updates) = (my_accum.value + noupdate)
                  my_n_thd_updates := my_n_thd_updates + 1

                }
          }}
          //println("<<<<<<<<<<<<<<<<< End of enquing updaates <<<<<<<<<<<<<<<<<")

          ////Store per thread updates
          Sequential {List.tabulate(n_thread) { thread =>

            val my_n_thd_updates = n_thd_updates(thread)
            val prev_thd:scala.Int = (thread + n_thread - 1) % n_thread

            val start_filled = my_n_thd_updates > 0
            val first_element_dst:Long = if (start_filled) thd_updates_dst(thread)(0) else -1
            val first_element_val = if (start_filled) thd_updates_val(thread)(0) else -1

            val last_element_prev_thd_filled = n_thd_updates_s(prev_thd).value

            val thd0 = (thread == 0)

            val single_dst = first_element_dst
            val single_val = first_element_val

            val prev_valid = (thd0 && (dst_prev != -1) || (!thd0 && last_element_prev_thd_filled))

            // first of current == last of previous

            val should_merge = prev_valid

            val last_element_prev_thd_dst:Long = if (thd0) dst_prev else  thd_updates_dst_s(prev_thd)
            val last_element_prev_thd_val:X = if (thd0) val_prev else  thd_updates_val_s(prev_thd)

            val same_dst = (first_element_dst == last_element_prev_thd_dst)

            val should_store_single = (prev_valid && start_filled && !same_dst) || (!prev_valid && start_filled)



            val merge_dst = last_element_prev_thd_dst
            val merge_val = mux(same_dst, last_element_prev_thd_val + first_element_val, last_element_prev_thd_val)

            //println(should_merge + " " + prev_valid + " " + !start_filled + " " + same_dst + " " + last_element_prev_thd_filled + " " + last_element_prev_thd_dst)

            //if (prev_valid) {
              //if (start_filled) {
                //if (same_dst) {
                  //// Merge prev + start
                //} else {
                  //// Store first
                  //// merge prev
                //}
              //} else {
                //// merge prev
              //}
            //} else {
               //// Store if start_filled
            //}

            if (should_store_single) {
              //println("ss: dst " + (single_dst - to) + " val: " + val_to_store(0))
              thd_updates_dst(thread)(0) = single_dst
              thd_updates_val(thread)(0) = single_val + noupdate
            }

            if (should_merge) {
                thd_updates_dst_w(thread).enq(merge_dst)
                thd_updates_val_w(thread).enq(merge_val)
                //println("merging: enquing write dst:" + (merge_dst - to) + " val: " + (merge_val))
            }


            if (should_store_single && my_n_thd_updates > 1) {
              OCnodes(thd_updates_dst(thread), my_n_thd_updates) scatter thd_updates_val(thread)
            } else if (my_n_thd_updates > 1) {
              OCnodes(thd_updates_dst(thread)(1 :: my_n_thd_updates), my_n_thd_updates - 1) scatter thd_updates_val(thread)(1 :: my_n_thd_updates)
            } else if (should_store_single) {
              OCnodes(thd_updates_dst(thread), 1) scatter thd_updates_val(thread)
            }
          }}

          if (n_thd_updates_s(n_thread - 1)) {
              //println("enquing write fringe dst: " + (fringe_dst - to) + " fringe_val " + fringe_val)
              thd_updates_dst_w(n_thread).enq(thd_updates_dst_s(n_thread - 1))
              thd_updates_val_w(n_thread).enq(thd_updates_val_s(n_thread - 1))
          }

          val my_write_almost_emptys = thd_updates_dst_w.map(_.numel == 1)
          //ud_start_full.zipWithIndex.map{ case (f, idx) => print(" "+f)}
          //
          //Combine cross-thread updates
          Sequential.Foreach((n_thread + 1) by 1) { thread =>

            val sels = List.tabulate(n_thread + 1){j => (thread == j)}

            val my_write_almost_empty = oneHotMux(sels, my_write_almost_emptys)

            if (my_write_almost_empty) {

              val f_dst = thd_updates_dst_w.zipWithIndex.map{ case (f, idx) => {if (idx == thread) f.deq() else 0L }}.reduce{_ | _}
              val f_val = thd_updates_val_w.zipWithIndex.map{ case (f, idx) => {if (idx == thread) f.deq() else 0 }}.reduce{_ | _}
              val f_full = ud_start_full.zipWithIndex.map{ case (f, idx) => {if (idx == thread) f.value else 0 }}.reduce{_ | _}

              if (f_dst != combine_cur_dst.value && combine_cur_dst.value != -1) {
                final_writes_dst.enq(combine_cur_dst)
                final_writes_val.enq(combine_accum + noupdate)
                //println("wf: dst: " + (combine_cur_dst - to) + " val: " + combine_accum)
                combine_accum := f_val
                combine_cur_dst := f_dst
              } else {
                if (!f_full) {
                  // If f_full is false, then should write the current buffer out regardless
                  val v = combine_accum + noupdate + f_val
                  final_writes_dst.enq(f_dst)
                  final_writes_val.enq(v)
                  //println("wf dst: " + (f_dst - to) + " val: " + v + " finished dst")
                  combine_cur_dst := -1
                  combine_accum := 0
                } else {
                  combine_accum := combine_accum + f_val
                  combine_cur_dst := f_dst
                }
              }
            }
          }

           //Transfer final
          dst_prev :=  combine_cur_dst // Adjusting address for next batch iteration
          val_prev := combine_accum.value

          val adjust = mux((combine_cur_dst == -1), combine_cur_dst, combine_cur_dst - to)
          //println("Left over dst: " + adjust + " val: " + val_prev)

          //Writing out combined results
          OCnodes(final_writes_dst) scatter final_writes_val


          if (last_iteration && combine_cur_dst != -1) {
            val val_to_store = SRAM[X](1)
            val_to_store(0) = (combine_accum + noupdate)
            OCnodes(combine_cur_dst :: combine_cur_dst + 1) store val_to_store
          }

          //println("-------------------End of batch iteration-------------------")
        }
      }

      probes(0::5) store probe_vals
    }

    val n_start:Int = if(iters % 2 == 0) 0 else n_nodes.to[Int]
    val result = getMem(OCnodes)
    val probes_out = getMem(probes)

    val probef = getArg(probe_out)
    val probe2f = getArg(probe_out2)
    println("probe: "+ probef)
    println("probe2: "+ probe2f)
    printArray(probes_out, "probes:" )
    for( j <- n_start until n_start + n_nodes.to[Int]) {

      //println((j - n_start) + "," + (result(j) * 1000000).to[Int])
      println((j - n_start) + "," + (result(j)))

    }



    closeBinary(f)
  }
}
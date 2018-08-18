package spatial.tests.feature.sparse


import spatial.dsl._


@spatial class BFS extends SpatialTest {
  override def runtimeArgs: Args = "6 10"

  val tileSize = 8000
  val edges_per_node = 6 // Will make this random later


  def bfs(nodesIn: Array[Int], edgesIn: Array[Int], countsIn: Array[Int], idsIn: Array[Int], n: Int, e: Int, average_nodes_per_edge: Int): Array[Int] = {
    val edges = DRAM[Int](e)
    val counts = DRAM[Int](n)
    val ids = DRAM[Int](n)
    val result = DRAM[Int](n)

    setMem(edges, edgesIn)
    setMem(counts, countsIn)
    setMem(ids, idsIn)

    val depth = ArgIn[Int]
    val d = args(1).to[Int]
    setArg(depth, d)
    val anpe = ArgIn[Int]
    setArg(anpe, average_nodes_per_edge)

    Accel {
      val frontierNodes = SRAM[Int](tileSize)
      val frontierCounts = SRAM[Int](tileSize)
      val frontierIds = SRAM[Int](tileSize)
      val frontierLevels = SRAM[Int](tileSize)
      val currentNodes = SRAM[Int](tileSize)
      val pieceMem = SRAM[Int](tileSize)

      val concatReg = Reg[Int](0)
      val numEdges = Reg[Int](1)

      // Flush first few for scatter safety
      Foreach(anpe by 1){i =>  //dummy read of anpe
        Parallel{
          frontierNodes(i) = 0.to[Int]
          // frontierCounts(i) = 0.to[Int]
          frontierLevels(i) = 0.to[Int]
          currentNodes(i) = 0.to[Int]
          pieceMem(i) = 0.to[Int]
        }
      }
      Parallel {
        frontierIds load ids(0 :: tileSize)
        frontierCounts load counts(0 :: tileSize)
      }

      Sequential.Foreach(depth.value by 1) { i => /* Loop 1 */
        val nextLen = Reg[Int](1)
        val nextId = Reg[Int](1)
        Sequential.Foreach(numEdges by 1) { k => /* Loop 2 */
          // val lastLen = Reg[Int](1)

          val fetch = currentNodes(k)
          // val lastFetch = currentNodes(k - 1)
          nextId := frontierIds(fetch)
          nextLen := frontierCounts(fetch)
          // lastLen := frontierCounts(lastFetch)

          // pieceMem load edges(nextId :: nextId + nextLen)
          pieceMem load edges(nextId :: nextId + nextLen)

          // val frontierAddr = SRAM[Int](tileSize)
          // Foreach(nextLen by 1) { kk =>
          Foreach(nextLen by 1) { kk =>
            /* Since Loop 2 is a metapipe and we read concatReg before
               we write to it, this means iter0 and iter1 both read
               0 in concatReg.  I.e. we always see the previous iter's
               value of concatReg, so we should add nextLen to it here
               if we are not on the first iter (since concatReg is and
               should be 0)
            */
            // val plus = mux(k == 0, 0.to[Int], anpe.value) // Really adding in lastLen
            val frontierAddr = kk + concatReg.value
            frontierNodes(frontierAddr) = pieceMem(kk)
          }
          concatReg := min(tileSize.to[Int], concatReg + nextLen)
        }

        Foreach(concatReg by 1) { kk => currentNodes(kk) = frontierNodes(kk) }
        Foreach(concatReg by 1) { kk => frontierLevels(kk) = i + 1 }
        result(currentNodes, concatReg) scatter frontierLevels
        numEdges := concatReg
        concatReg := 0.to[Int]
      }
    }

    getMem(result)
  }


  def main(args: Array[String]): Unit = {
    /* NEW VERSION FOR PERFORMANCE MEASUREMENTS */
    val E = 9600000
    val N = tileSize
    val average_nodes_per_edge = args(0).to[Int]
    val spacing = 3
    val ed = E //args(1).to[SInt] // Set to roughly max_edges_per_node * N

    val OCnodes = Array.tabulate(N) {i => 0.to[Int]}
    val OCedges = Array.tabulate(ed){ i => i*2 % N}
    val OCids = Array.tabulate(N)( i => average_nodes_per_edge*average_nodes_per_edge*i+1 % E)
    val OCcounts = Array.tabulate(N){ i => average_nodes_per_edge}// random[Int](average_nodes_per_edge-1)*2+1}

    val result = bfs(OCnodes, OCedges, OCcounts, OCids, N, E, average_nodes_per_edge)
    val gold = Array.tabulate(args(1).to[Int] + 1){i => 
      val numnodes = if (i == 0) 1 else Array.tabulate(i){j => average_nodes_per_edge}.reduce{_*_}
      numnodes * i
    }.reduce{_+_}
    // val gold = (6*1) + (16*2) + (22*3) + (5*4)
    // println("Cksum: " + gold + " == " + result.reduce{_+_})

    val cksum = gold == result.reduce{_+_}
    printArray(result, "result: ")
    println("Cksum = " + result.reduce{_+_})
    assert(cksum)
    // println("PASS: " + cksum + " (BFS)")
  }

}

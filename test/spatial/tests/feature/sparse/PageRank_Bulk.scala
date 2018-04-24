// package spatial.tests.feature.sparse


// import spatial.dsl._


// @test class PageRank_Bulk extends SpatialTest { // ReviveMe (store to var)
//   override def runtimeArgs: Args = "50 0.125"

//   type Elem = FixPt[TRUE,_16,_16] // Float
//   type X = FixPt[TRUE,_16,_16] // Float

//   /*
//     Currently testing with DIMACS10 Chesapeake dataset from UF Sparse Matrix collection

//   */
//   val margin = 0.3f


//   def main(args: Array[String]): Unit = {
//     val sparse_data = loadCSV2D[Int]("/remote/regression/data/machsuite/pagerank_chesapeake.csv", " ", "\n").t
//     val rows = sparse_data(0,0)
//     val node1_list = Array.tabulate(sparse_data.cols - 1){i => sparse_data(0, i+1)-1} // Every page is 1-indexed...
//     val node2_list = Array.tabulate(sparse_data.cols - 1){i => sparse_data(1, i+1)-1} // Every page is 1-indexed...
//     // Preprocess to get frontier sizes.  We can do this on chip also if we wanted
//     println("Matrix has " + rows + " rows")
//     val edgeLens = Array.tabulate(rows){i => Array.tabulate(node1_list.length){j =>
//       if (node1_list(j) == i) 1 else 0
//     }.reduce{_+_}}
//     val edgeIds = Array.tabulate(rows){i =>
//       var id: Int = -1
//       Array.tabulate(node1_list.length){j =>
//         if (id == -1 && node1_list(j) == i) {
//           id = j
//           j
//         } else { 0 }
//       }.reduce{_+_}}

//     // printArray(node1_list, "node1_list:")
//     // printArray(node2_list, "node2_list:")
//     printArray(edgeLens, "edgeLens: ")
//     printArray(edgeIds, "edgeIds: ")

//     val tileSize = 16 (16 -> 16 -> 128)
//     val par_load = 1
//     val par_store = 1
//     val tile_par = 1 (1 -> 1 -> 12)
//     val page_par = 1 (1 -> 1 -> 16)

//     // Arguments
//     val itersIN = args(0).to[Int]
//     val dampIN = args(1).to[X]

//     val iters = ArgIn[Int]
//     val NP    = ArgIn[Int]
//     val damp  = ArgIn[X]
//     val NE    = ArgIn[Int]
//     setArg(iters, itersIN)
//     setArg(NP, rows)
//     setArg(damp, dampIN)
//     setArg(NE, node2_list.length)

//     val OCpages    = DRAM[X](NP)
//     val OCedges    = DRAM[Int](NE)    // srcs of edges
//     val OCedgeLens   = DRAM[Int](NP)    // counts for each edge
//     val OCedgeIds   = DRAM[Int](NP) // Start index of edges

//     val pagesInit = Array.tabulate(NP){i => 4.to[X]}

//     setMem(OCpages, pagesInit)
//     setMem(OCedges, node2_list)
//     setMem(OCedgeLens, edgeLens)
//     setMem(OCedgeIds, edgeIds)

//     Accel {
//       Sequential.Foreach(iters by 1){iter =>
//         // Step through each tile
//         Foreach(NP by tileSize par tile_par){page =>
//           val local_pages = SRAM[X](tileSize).buffer
//           val local_edgeIds = SRAM[Int](tileSize)
//           val local_edgeLens = SRAM[Int](tileSize)
//           val pages_left = min(tileSize.to[Int], NP-page)
//           local_pages load OCpages(page::page+pages_left par par_load)
//           local_edgeLens load OCedgeLens(page::page+pages_left par par_load)
//           local_edgeIds load OCedgeIds(page::page+pages_left par par_load)
//           // Process each page in local tile
//           Sequential.Foreach(pages_left by 1 par page_par){local_page =>
//             // Fetch edge list for this page
//             val edgeList = FIFO[Int](128)
//             val id = local_edgeIds(local_page)
//             val len = local_edgeLens(local_page)
//             edgeList load OCedges(id::id+len)
//             // Triage between edges that exist in local tiles and off chip
//             val nearPages = FIFO[Int](128)
//             val farPages = FIFO[Int](128)
//             Foreach(edgeList.numel by 1){i =>
//               val tmp = edgeList.deq()
//               if (tmp >= page && tmp < page+pages_left) {
//                 nearPages.enq(tmp - page)
//               } else {
//                 farPages.enq(tmp)
//               }
//             }
//             // Fetch off chip info
//             val local_farPages = FIFO[X](128)
//             val local_farEdgeLens = FIFO[Int](128)
//             Foreach(farPages.numel by 1){ i =>
//               val el = farPages.deq()
//               local_farPages load OCpages(el::el+1)
//               local_farEdgeLens load OCedgeLens(el::el+1)
//             }

//             // Do math to find new rank
//             // val pagerank = Pipe.II(7).Reduce(Reg[X](0))(len by 1){i =>
//             val pagerank = Pipe.Reduce(Reg[X](0))(len by 1){i =>
//               if (nearPages.isEmpty) {
//                 println("page: " + page + ", local_page: " + local_page + " deq from far")
//                 local_farPages.deq() / local_farEdgeLens.deq().to[X]
//               } else {
//                 val addr = nearPages.deq()
//                 println("page: " + page + ", local_page: " + local_page + " deq from near addr " + addr)
//                 local_pages(addr) / local_edgeLens(addr).to[X]
//               }
//             }{_+_}

//             // Write new rank
//             local_pages(local_page) = pagerank * damp + (1.to[X] - damp)
//           }
//           OCpages(page::page+pages_left par par_store) store local_pages
//         }
//       }
//     }

//     val result = getMem(OCpages)

//     val gold = Array.empty[X](NP)
//     // Init
//     for (i <- 0 until NP) {
//       gold(i) = pagesInit(i)
//     }

//     // Really bad imperative version
//     for (ep <- 0 until iters) {
//       // println("Iter " + ep)
//       for (i <- 0 until NP) {
//         val numEdges = edgeLens(i)
//         val startId = edgeIds(i)
//         val iterator = Array.tabulate(numEdges){kk => startId + kk}
//         val these_edges = iterator.map{j => node2_list(j)}
//         val these_pages = these_edges.map{j => gold(j)}
//         val these_counts = these_edges.map{j => edgeLens(j)}
//         val pr = these_pages.zip(these_counts){ (p,c) =>
//           // println("page " + i + " doing " + p + " / " + c)
//           p/c.to[X]
//         }.reduce{_+_}
//         // println("new pr for " + i + " is " + pr)
//         gold(i) = pr*dampIN + (1.to[X]-dampIN)
//       }
//     }

//     println("PageRank on DIMACS10 Chesapeake dataset downloaded from UF Sparse Matrix collection")
//     printArray(gold, "gold: ")
//     printArray(result, "result: ")
//     val cksum = result.zip(gold){ case (o, g) => (g < (o + margin.to[X])) && g > (o - margin.to[X])}.reduce{_&&_}
//     println("PASS: " + cksum + " (PageRank)")

//   }

// }


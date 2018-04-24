// package spatial.tests.feature.memories.fifo


// import spatial.dsl._


// @test class CompactingFIFO extends SpatialTest { // ReviveMe (write to var)
//   override def runtimeArgs: Args = "640"

//   val tileSize = 64


//   def main(args: Array[String]): Unit = {
//     val arraySize = args(0).to[Int]

//     val bitmask = Array.tabulate(arraySize) { i => random[Int](2)}
//     printArray(bitmask, "Bitmask: ")

//     val P1 = 4 (16 -> 16)

//     val N = ArgIn[Int]
//     setArg(N, arraySize)

//     val bitmaskDRAM = DRAM[Int](N)
//     setMem(bitmaskDRAM, bitmask)
//     val out = DRAM[Int](N)

//     Accel{
//       Sequential.Foreach(N by tileSize){ i =>
//         val bitmasks = SRAM[Int](tileSize)
//         val fifo = FIFO[Int](tileSize)
//         bitmasks load bitmaskDRAM(i :: i + tileSize)

//         // Load while respecting bitmask
//         Foreach(tileSize by 1 par P1){ j =>
//           fifo.enq(i+j, bitmasks(j) == 1)
//         }

//         // Fill remainder with 0s
//         FSM(0)(filler => filler != 1){filler =>
//           if (!fifo.isFull) {
//             Pipe{fifo.enq(-1)}
//           }
//         }{ filler => mux(fifo.isFull, 1, 0)}

//         // Store back
//         out(i :: i + tileSize par 2) store fifo
//       }
//     }

//     val result = getMem(out)

//     val gold = Array.empty[Int](arraySize)
//     var head = 0
//     var tail = tileSize-1
//     for (j <- 0 until arraySize by tileSize) {
//       head = 0
//       tail = tileSize-1
//       for (k <- 0 until tileSize) {
//         if (bitmask(j+k) == 1){
//           gold(j+head) = j+k
//           head = head + 1
//         } else {
//           gold(j+tail) = -1
//           tail = tail - 1
//         }
//       }
//     }
//     printArray(gold, "gold: ")
//     printArray(result, "got: ")
//     assert(result == gold)
//   }
// }

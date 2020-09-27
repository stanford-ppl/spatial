package spatial.tests.feature.memories.fifo

import spatial.dsl._

@spatial class FIFOVector extends SpatialTest {
  override def dseModelArgs: Args = "960"
  override def finalModelArgs: Args = "960"
  override def runtimeArgs: Args = "960"

  def main(args: Array[String]): Unit = {

    val tileSize = 64
    
    val dstFPGA = DRAM[I64](8)
    val dummy = ArgOut[Bit]

    Accel {
      val f1 = FIFO[I16](tileSize)
      val f2 = FIFO[I16](tileSize)
      val f3 = FIFO[I64](tileSize)
      Stream.Foreach(8 by 1) { i => 
        // Put 8 elements in FIFO
        Pipe{
          val data = Vec.ZeroFirst(i.to[I16]*4, i.to[I16]*4+1, i.to[I16]*4+2, i.to[I16]*4+3)
          f1.enqVec(data)  // 0, 1, 2, 3  ---> fifo  (0 is first)
        }

        // Modify 4 elements in FIFO
        val comm = FIFOReg[Bit]
        Pipe{
          Sequential.Foreach(4 by 1){j => 
            val acc = Reg[I64](0)
            val data = f1.deq()
            f2.enq(data*2)
          }
          comm.enq(true)
        }

        // Make thicc and put in chonky FIFO
        Pipe{
          dummy := comm.deq()
          val data = cat(f2.deqVec(4).asBits).as[I64]  // fifo ---> 6, 4, 2, 0 (0 is least sig digit)
          f3.enq(data)
        }
      }
      // Store FIFO
      Pipe{
        dstFPGA store f3
      }

    }
    val dst = getMem(dstFPGA)

    val gold = Array.tabulate(8){i => ((i.to[I64]*4+3)*2 << 48) + ((i.to[I64]*4+2)*2 << 32) + ((i.to[I64]*4+1)*2 << 16) + ((i.to[I64]*4)*2)}

    printArray(gold, "Wanted:")
    printArray(dst, "Got:")

    println(r"PASS: ${gold == dst} (FIFOVector)")
    assert(gold == dst)
  }
}

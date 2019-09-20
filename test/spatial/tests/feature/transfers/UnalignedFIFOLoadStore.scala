package spatial.tests.feature.transfers
import spatial.dsl._

@spatial class UnalignedFIFOLoadStore extends SpatialTest {
  val N = 1024

  def main(args: Array[String]): Unit = {
    type T = Int

    val src = Array.tabulate[T](N) {i => i.to[T] }

    val dram = DRAM[T](N)
    val dram2 = DRAM[T](N,N)
    setMem(dram, src)
    Accel {
      Foreach(N by 1) { i =>
        val fifo = FIFO[T](N)
        val size = Reg[T]
        Pipe {
          size := max(3*i % (N-i),1)
        }
        fifo load dram(i::i+size.value)
        dram2(i,0::size.value) store fifo
        val fifo2 = FIFO[T](N)
        Foreach(size.value::N) { j =>
          fifo2.enq(0)
        }
        dram2(i,size.value::N) store fifo2
      }
    }
    val out = getMem(dram2)

    val goldArray = (0 until N) { i => 
      val size = max(3*i % (N-i),1)
      (0 until N) { j => 
        if (j < size) src(i+j) else 0.to[T]
      }
    }
    val gold = goldArray.flatten

    writeCSV1D(out, "out.csv", delim="\n")
    writeCSV1D(gold, "gold.csv", delim="\n")

    val cksum = approxEql(out, gold, 0)
    println("PASS: " + cksum + " (UnalignedFIFOLoadStore)")
    assert(cksum)
  }
}

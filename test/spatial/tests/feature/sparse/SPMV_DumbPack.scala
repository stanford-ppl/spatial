package spatial.tests.feature.sparse

import spatial.dsl._



@test class SPMV_DumbPack extends SpatialTest {
  override def runtimeArgs: Args = "1536"

  type T = Int //FixPt[Signed,B16,B16]

  val pp = 3840
  val NNZ = 60

  val ip = 1
  val op = 2

  val tileSize = 384

  val margin = 1


  def main(args: Array[String]): Unit = {
    val nn = args(0).to[Int]
    val P = pp

    val AC = Array.tabulate(nn){ i => Array.tabulate(NNZ) { j => (j * 3).to[Int]}}
    val AD = Array.tabulate(nn){ i => Array.fill(NNZ) {random[Int](5) }}
    val S = Array.tabulate(nn){ i => NNZ.to[Int] }
    val V = Array.tabulate(P){ i => i.to[Int] }

    val N = ArgIn[Int]
    setArg(N,nn)

    val aC = DRAM[Int](pp,NNZ)
    val aD = DRAM[Int](pp,NNZ)
    val sizes = DRAM[Int](pp)
    val v = DRAM[Int](pp)
    val out = DRAM[Int](N)

    //val op = op (1 -> 6)
    //val ip = ip (1 -> 96)
    val stPar    = ip (1 -> 1)

    setMem(aC, AC.flatten)
    setMem(aD, AD.flatten)
    setMem(sizes, S)
    setMem(v, V)

    Accel {
      Foreach(N by tileSize par op){ rowchunk =>
        val smvresult = SRAM[Int](tileSize)
        val smvtileSizes = SRAM[Int](tileSize)
        smvtileSizes load sizes(rowchunk :: rowchunk+tileSize par ip)
        Foreach(tileSize by 1){row =>
          val csrCols = SRAM[Int](tileSize)
          val csrData = SRAM[Int](tileSize)
          val vecGathered = SRAM[Int](tileSize)

          // Load dense csr piece
          val len = smvtileSizes(row)
          val OCROW = (rowchunk+row) // TODO: Issue #47
          Parallel{
            csrCols load aC(OCROW, 0 :: len par ip)
            csrData load aD(OCROW, 0 :: len par ip)
          }
          vecGathered gather v(csrCols, len)

          val acc = Reduce(Reg[Int](0.to[Int]))(len by 1 par ip) { i =>
            csrData(i) * vecGathered(i)
          }{_+_}

          smvresult(row) = acc.value
        }
        out(rowchunk::rowchunk+tileSize par stPar) store smvresult
      }
    }
    val smvresult = getMem(out)



    val gold = AC.zip(AD) { (col, data) => col.zip(data) {(c, d) =>
      d*V(c)
    }.reduce{_+_}}

    printArray(gold, "gold: ")
    printArray(smvresult, "smvresult: ")

    val cksum = smvresult.zip(gold){(a,b) => a - margin < b && a + margin > b}.reduce{_&&_}
    println("PASS: " + cksum + " (SMV)")
    assert(cksum)
  }
}
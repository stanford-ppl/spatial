package spatial.tests.plasticine

import spatial.dsl._

class GEMM_Blocked1 extends GEMM_Blocked {
  val dim = 128
  val ts = 32 
  val its = 32
  val loop_ii = 2
  val loop_jj = 2
  val loop_kk = 1
  val loop_i = 1
  val loop_j = 1
}

class GEMM_Blocked2 extends GEMM_Blocked {
  val dim = 128
  val ts = 32 
  val its = 32
  val loop_ii = 1
  val loop_jj = 1
  val loop_kk = 2
  val loop_i = 2
  val loop_j = 2
}

class GEMM_BlockedNoPar extends GEMM_Blocked {
  val dim = 128 // param [512]
  val ts = 32 // param [256] # (64, 256, 64)
  val its = 32 // param [128] # pmuSize / <ts>
  val loop_ii = 1 // param [1] #(1, <dim> / <its>, 2)
  val loop_jj = 1 // param [1] #(1, <dim> / <ts>, 2)
  val loop_kk = 1 // param [1] #(1, <dim> / <ts>, 2)
  val loop_i = 1 // param [1] #(1, 6, 2)
  val loop_j = 1 // param (8, 16, 8) | <ts> % p == 0
}

@spatial abstract class GEMM_Blocked extends PlasticineTest { // Regression (Dense) // Args: 128
                                                                                                  
  val dim:Int
  val ts:Int
  val its:Int
  val loop_ii:Int
  val loop_jj:Int
  val loop_kk:Int
  val loop_i:Int
  val loop_j:Int

  val ip = 16
  type T = FixPt[TRUE,_16,_16] // Fatter type so that ts is burst aligned

  def gemm(a_data:Matrix[T], b_data:Matrix[T], c_init:Matrix[T]) = {

    val a_dram = DRAM[T](dim,dim)
    val b_dram = DRAM[T](dim,dim)
    val c_dram = DRAM[T](dim,dim)

    setMem(a_dram, a_data)
    setMem(b_dram, b_data)
    setMem(c_dram, c_init)

    Accel{
      Foreach(dim by its par loop_ii, dim by ts par loop_jj) { (ii, jj) =>
        val c_col = SRAM[T](its,ts)
        MemReduce(c_col par ip)(dim by ts par loop_kk) { kk => 
          val c_col_partial = SRAM[T](its,ts)
          val b_sram = SRAM[T](ts,ts)
          b_sram load b_dram(kk::kk+ts, jj::jj+ts par ip)
          Foreach(its by 1 par loop_i) { i => 
            val a_sram = SRAM[T](ts)
            a_sram load a_dram(ii+i, kk::kk+ts par ip)
            Foreach(ts by 1 par loop_j) { j => 
              val dot = Reduce(Reg[T])(ts by 1 par ip) { k =>
                b_sram(k, j) * a_sram(k)
              } { _ + _ }
              Pipe { c_col_partial(i,j) = dot }
            }
          }
          c_col_partial
        }{_+_}
        c_dram(ii::ii+its, jj::jj+ts par ip) store c_col
      }
    }
    getMatrix(c_dram)
  }

  def main(args: Array[String]): Unit = {

    val a_data = (0::dim,0::dim){(i,j) => random[T](5)}
    val b_data = (0::dim,0::dim){(i,j) => random[T](5)}
    // val a_data = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_a.csv", "\n").reshape(dim,dim)
    // val b_data = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_b.csv", "\n").reshape(dim,dim)
    val c_init = (0::dim, 0::dim){(i,j) => 0.to[T]}


    // val c_gold = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_gold.csv", "\n").reshape(dim,dim)
    val c_gold = (0::dim,0::dim){(i,j) => 
      Array.tabulate(dim){k => a_data(i,k) * b_data(k,j)}.reduce{_+_}
    }
    val c_result = gemm(a_data, b_data, c_init)

    printMatrix(c_gold, "C Gold: ")
    printMatrix(c_result, "C Result: ")

    val margin = 0.5.to[T]
    val cksum = c_gold.zip(c_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    println("PASS: " + cksum + " (GEMM_Blocked)")
    assert(cksum)
  }
}


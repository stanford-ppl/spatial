package spatial.tests.plasticine

import spatial.dsl._

@spatial class GEMM_Blocked extends SpatialTest { // Regression (Dense) // Args: 128
                                                                                                  
  val dim = 128 // param [512]
  val idim = 128 // param [256]
  val DIM = dim
  val IDIM = idim

  val ts = 64 // param [256] # (64, 256, 64)
  val its = 64 // param [128] # pmuSize / <ts>
  val loop_ii = 1 // param [1] #(1, <DIM> / <its>, 2)
  val loop_jj = 1 // param [1] #(1, <DIM> / <ts>, 2)
  val loop_kk = 1 // param [1] #(1, <DIM> / <ts>, 2)
  val loop_i = 1 // param [1] #(1, 6, 2)
  val loop_k = 8 // param (8, 16, 8) | <ts> % p == 0

  val ip = 16
  type T = FixPt[TRUE,_16,_16] // Fatter type so that ts is burst aligned

  def gemm(a_data:Matrix[T], b_data:Matrix[T], c_init:Matrix[T]) = {

    //val dim = ArgIn[Int]
    //val idim = ArgIn[Int]
    //setArg(dim, a_data.rows); bound(dim) = DIM
    //setArg(idim, IDIM); bound(idim) = IDIM
    val a_dram = DRAM[T](idim,dim)
    val b_dram = DRAM[T](dim,dim)
    val c_dram = DRAM[T](idim,dim)

    setMem(a_dram, a_data)
    setMem(b_dram, b_data)
    setMem(c_dram, c_init)

    Accel{
      Foreach(idim by its par loop_ii) { ii => // this loop defenitilely cant be parallelized right now
        Foreach(dim by ts par loop_jj) { jj => 
          val c_col = SRAM[T](its,ts)
          MemReduce(c_col par ip)(dim by ts par loop_kk) { kk => 
            val c_col_partial = SRAM[T](its,ts)
            val b_sram = SRAM[T](ts,ts)
            b_sram load b_dram(kk::kk+ts, jj::jj+ts par ip)
            Foreach(its by 1 par loop_i) { i => 
              val a_sram = SRAM[T](ts)
              a_sram load a_dram(ii+i, kk::kk+ts par ip)
              val c_tmp = SRAM[T](ts)
              MemReduce(c_tmp par ip)(ts by 1 par loop_k) { k => 
                val c_tmp_partial = SRAM[T](ts)
                val temp_a = a_sram(k)
                Foreach(ts by 1 par ip) { j => c_tmp_partial(j) = b_sram(k, j) * temp_a }
                c_tmp_partial
              }{_+_}
              Foreach(ts by 1 par ip){cpy => c_col_partial(i,cpy) = c_tmp(cpy)}
            }
            c_col_partial
          }{_+_}
          c_dram(ii::ii+its, jj::jj+ts par ip) store c_col
        }
      }
    }
    getMatrix(c_dram)
  }

  def main(args: Array[String]): Unit = {

    val a_data = (0::DIM,0::DIM){(i,j) => random[T](5)}
    val b_data = (0::DIM,0::DIM){(i,j) => random[T](5)}
    // val a_data = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_a.csv", "\n").reshape(dim,dim)
    // val b_data = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_b.csv", "\n").reshape(dim,dim)
    val c_init = (0::DIM, 0::DIM){(i,j) => 0.to[T]}


    // val c_gold = loadCSV1D[T](sys.env("SPATIAL_HOME") + "/apps/data/gemm/gemm_gold.csv", "\n").reshape(dim,dim)
    val c_gold = (0::DIM,0::DIM){(i,j) => 
      Array.tabulate(DIM){k => a_data(i,k) * b_data(k,j)}.reduce{_+_}
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


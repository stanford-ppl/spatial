package spatial.tests.feature.dense

import spatial.dsl._
import spatial.lib.BLAS

@spatial class BasicBLAS extends SpatialTest {
  override def runtimeArgs: Args = "0.2 0.8 64 128 96"

  // DSE Parameters
  val tileSize  = 16 (16 -> 16 -> 1024)
  val outer_par = 1 (1 -> 1 -> 32)
  val inner_par = 1 (1 -> 1 -> 16)
  val load_par  = 8 (1 -> 1 -> 16)
  val store_par = 8 (1 -> 1 -> 16)

  // gemm and gemmv specific
  val tileSizeN    = 16 (16 -> 16 -> 1024)
  val tileSizeM    = 16 (16 -> 16 -> 1024)
  val tileSizeK    = 16 (16 -> 16 -> 1024)
  val m_inner_par  = 1 (1 -> 1 -> 8)
  val n_inner_par  = 1 (1 -> 1 -> 8)
  val k_inner_par  = 1 (1 -> 1 -> 8)
  val m_outer_par  = 1 (1 -> 1 -> 8)
  val n_outer_par  = 1 (1 -> 1 -> 8)
  val k_outer_par  = 1 (1 -> 1 -> 8)
  val c_reduce_par = 1 (1 -> 1 -> 8)
  val y_reduce_par = 1 (1 -> 1 -> 8)


  def Dot[T:Num](N: Reg[Int],
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int,
    res: Reg[T]): Unit = {
    // Loop over whole vectors
    val outer_res = Reduce(Reg[T])(N.value by tileSize par outer_par){i =>
      // Compute elements left in this tile
      val elements = min(tileSize, N.value - i)
      // Create onchip structures
      val x_tile = SRAM[T](tileSize)
      val y_tile = SRAM[T](tileSize)
      // Load local tiles
      x_tile load X(i::i+elements par load_par)
      y_tile load Y(i::i+elements par load_par)
      // Loop over elements in local tiles
      val inner_res = Reduce(Reg[T])(elements by 1 par inner_par){j =>
        x_tile(j) * y_tile(j)
      }{_+_}
      inner_res
    }{_+_}
    res := outer_res

  }


  def Axpy[T:Num](N: Reg[Int], alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int,
    res: DRAM1[T]): Unit = {
    // Loop over whole vectors
    Foreach(N.value by tileSize par outer_par){i =>
      // Compute elements left in this tile
      val elements = min(tileSize, N.value - i)
      // Create onchip structures
      val x_tile = SRAM[T](tileSize)
      val y_tile = SRAM[T](tileSize)
      val z_tile = SRAM[T](tileSize)
      // Load local tiles
      x_tile load X(i::i+elements par load_par)
      y_tile load Y(i::i+elements par load_par)
      // Loop over elements in local tiles
      Foreach(elements by 1 par inner_par){j =>
        z_tile(j) = alpha * x_tile(j) + y_tile(j)
      }
      // Store tile to DRAM
      res(i::i+elements par store_par) store z_tile
    }
  }


  def Gemm[T:Num](M: Reg[Int], N: Reg[Int], K: Reg[Int],
    alpha: T,
    A: DRAM2[T], lda: Int,
    B: DRAM2[T], ldb: Int,
    beta: T,
    C: DRAM2[T], ldc: Int): Unit = {
    Foreach(M.value by tileSizeM par m_outer_par){i =>
      // Compute leftover dim
      val elements_m = min(tileSizeM, M.value - i)
      Foreach(N.value by tileSizeN par n_outer_par){j =>
        // Compute leftover dim
        val elements_n = min(tileSizeN, N.value - j)
        // Create C tile for accumulating
        val c_tile = SRAM[T](tileSizeM, tileSizeN)
        MemReduce(c_tile par c_reduce_par)(K.value by tileSizeK par k_outer_par){l =>
          // Create local C tile
          val c_tile_local = SRAM[T](tileSizeM, tileSizeN)
          // Compute leftover dim
          val elements_k = min(tileSizeK, K.value - l)
          // Generate A and B tiles
          val a_tile = SRAM[T](tileSizeM, tileSizeK)
          val b_tile = SRAM[T](tileSizeK, tileSizeN)
          // Transfer tiles to sram
          Parallel{
            a_tile load A(i::i+elements_m, l::l+elements_k par load_par)
            b_tile load B(l::l+elements_k, j::j+elements_n par load_par)
          }
          Foreach(elements_m by 1 par m_inner_par){ii =>
            Foreach(elements_n by 1 par n_inner_par){jj =>
              c_tile_local(ii,jj) = Reduce(Reg[T])(elements_k by 1 par k_inner_par){ll =>
                a_tile(ii,ll) * b_tile(ll,jj)
              }{_+_}
            }
          }
          c_tile_local
        }{_+_}
        C(i::i+elements_m, j::j+elements_n par store_par) store c_tile
      }
    }
  }


  def Gemv[T:Num](M: Reg[Int], N: Reg[Int],
    alpha: T,
    A: DRAM2[T], lda: Int,
    X: DRAM1[T], incX: Int,
    beta: T,
    Y: DRAM1[T], incY: Int): Unit = {
    Foreach(M.value by tileSizeM par m_outer_par){i =>
      // Compute leftover dim
      val elements_m = min(tileSizeM, M.value - i)
      // Create Y tile
      val y_tile = SRAM[T](tileSizeM)
      MemReduce(y_tile par y_reduce_par)(N.value by tileSizeN par n_outer_par){j =>
        // Compute leftover dim
        val elements_n = min(tileSizeN, N.value - j)
        // Create local Y tile for accumulating
        val y_tile_local = SRAM[T](tileSizeM)
        // Create X tile
        val x_tile = SRAM[T](tileSizeN)
        // Load vector tile
        x_tile load X(j::j+elements_n par load_par)
        // Create A tile
        val a_tile = SRAM[T](tileSizeM, tileSizeN)
        // Load matrix tile
        a_tile load A(i::i+elements_m, j::j+elements_n par load_par)
        Foreach(elements_m by 1 par m_inner_par){ii =>
          y_tile_local(ii) = Reduce(Reg[T])(elements_n by 1 par n_inner_par){jj =>
            a_tile(ii,jj) * x_tile(jj)
          }{_+_}
        }
        y_tile_local
      }{_+_}
      Y(i::i+elements_m par store_par) store y_tile
    }
  }


  def Ger[T:Num](M: Reg[Int], N: Reg[Int],
    alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int,
    A: DRAM2[T], lda: Int): Unit = {
    Foreach(M.value by tileSizeM par m_outer_par){i =>
      // Compute leftover dim
      val elements_m = min(tileSizeM, M.value - i)
      // Create X tile
      val x_tile = SRAM[T](tileSizeM)
      // Load x data into tile
      x_tile load X(i::i+elements_m)
      Foreach(N.value by tileSizeN par n_outer_par){j =>
        // Compute leftover dim
        val elements_n = min(tileSizeN, N.value - j)
        // Create Y and A tiles
        val y_tile = SRAM[T](tileSizeN)
        val a_tile = SRAM[T](tileSizeM, tileSizeN)
        // Load x data into tile
        y_tile load Y(j::j+elements_n)
        Foreach(elements_m by 1 par m_inner_par){ii =>
          Foreach(elements_n by 1 par n_inner_par){jj =>
            a_tile(ii,jj) = x_tile(ii) * y_tile(jj)
          }
        }
        A(i::i+elements_m, j::j+elements_n par store_par) store a_tile
      }
    }
  }


  def Scal[T:Num](N: Reg[Int], alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T]): Unit = {
    // Loop over whole vectors
    Foreach(N.value by tileSize par outer_par){i =>
      // Compute elements left in this tile
      val elements = min(tileSize, N.value - i)
      // Create onchip structures
      val x_tile = SRAM[T](tileSize)
      val y_tile = SRAM[T](tileSize)
      // Load local tiles
      x_tile load X(i::i+elements par load_par)
      // Loop over elements in local tiles
      Foreach(elements by 1 par inner_par){j =>
        y_tile(j) = alpha * x_tile(j)
      }
      // Store tile to DRAM
      Y(i::i+elements par store_par) store y_tile
    }
  }


  def Axpby[T:Num](N: Reg[Int],
    alpha: T,
    X: DRAM1[T], incX: Int,
    beta: T,
    Y: DRAM1[T], incY: Int,
    Z: DRAM1[T]): Unit = {
    Scal[T](N, beta, Y, incY, Z)
    Axpy[T](N, alpha, X, incX, Z, incY, Z)
  }

  type T = FixPt[TRUE,_16,_16]


  def main(args: Array[String]): Unit = {

    // cmd-line args (i.e.- "20 0.5 0.5 64 64 64")
    val alpha  = args(0).to[T]
    val beta   = args(1).to[T]
    val dim_M = args(2).to[Int]
    val dim_N = args(3).to[Int]
    val dim_K = args(4).to[Int]

    // Create random data structures
    val X_data = Array.tabulate(dim_N){i => random[T](3)}
    val ger_X_data = Array.tabulate(dim_M){i => random[T](3)}
    val Y_data = Array.tabulate(dim_N){i => random[T](3)}
    val matrix_A = (0::dim_M,0::dim_K){(i,j) => random[T](3)}
    val matrix_B = (0::dim_K,0::dim_N){(i,j) => random[T](3)}
    val init_matrix_C = (0::dim_M,0::dim_N){(i,j) => 0.to[T]}
    val gemv_X_data = Array.tabulate(dim_K){i => random[T](3)}
    val init_vec_Y = Array.tabulate(dim_M){i => 0.to[T]}

    // Offchip structures
    val a = ArgIn[T]
    val b = ArgIn[T]
    val dot = ArgOut[T]
    val KK = ArgIn[Int]
    val NN = ArgIn[Int]
    val MM = ArgIn[Int]
    setArg(a, alpha)
    setArg(b, beta)
    setArg(MM, dim_M)
    setArg(NN, dim_N)
    setArg(KK, dim_K)
    val X = DRAM[T](NN)
    val ger_X = DRAM[T](MM)
    val Y = DRAM[T](NN)
    val axpby_Z = DRAM[T](NN)
    val A = DRAM[T](MM,KK)
    val B = DRAM[T](KK,NN)
    val C = DRAM[T](MM,NN)
    val gemv_X = DRAM[T](KK)
    val gemv_Y = DRAM[T](MM)
    val ger_A = DRAM[T](MM,NN)
    val scal_Y = DRAM[T](NN)
    val axpy = DRAM[T](NN)
    setMem(X, X_data)
    setMem(ger_X, ger_X_data)
    setMem(Y, Y_data)
    setMem(A, matrix_A)
    setMem(B, matrix_B)
    setMem(C, init_matrix_C)
    setMem(gemv_X, gemv_X_data)
    setMem(gemv_Y, init_vec_Y)

    // Run Accel functions
    Accel{
      // Use defs from spatial's BLAS lib
      dot := BLAS.Dot[T](NN, X, 1, Y, 1)
      BLAS.Axpy[T](NN, a, X, 1, Y, 1, axpy)
      BLAS.Gemm[T](MM, NN, KK, a, A, A.cols, B, B.cols, b, C, C.cols)
      BLAS.Gemv[T](MM, KK, a, A, A.cols, gemv_X, 1, b, gemv_Y, 1)
      BLAS.Ger[T](MM, NN, a, ger_X, 1, Y, 1, ger_A, ger_A.cols)
      BLAS.Scal[T](NN, a, X, 1, scal_Y)
      BLAS.Axpby[T](NN, a, X, 1, b, Y, 1, axpby_Z)

      // // Use defs in the app
      // Dot[T](NN, X, 1, Y, 1, dot)
      // Axpy[T](NN, a, X, 1, Y, 1, axpy)
      // Gemm[T](MM, NN, KK, a, A, A.cols, B, B.cols, b, C, C.cols)
      // Gemv[T](MM, KK, a, A, A.cols, gemv_X, 1, b, gemv_Y, 1)
      // Ger[T](MM, NN, a, ger_X, 1, Y, 1, ger_A, ger_A.cols)
      // Scal[T](NN, a, X, 1, scal_Y)
      // Axpby[T](NN, a, X, 1, b, Y, 1, axpby_Z)
    }

    // Get results
    val dot_res = getArg(dot)
    val axpy_res = getMem(axpy)
    val gemm_res = getMatrix(C)
    val gemv_res = getMem(gemv_Y)
    val ger_res = getMatrix(ger_A)
    val scal_res = getMem(scal_Y)
    val axpby_res = getMem(axpby_Z)

    // Compute Golds
    val dot_gold = X_data.zip(Y_data){_*_}.reduce{_+_}
    val axpy_gold = X_data.zip(Y_data){case (x,y) => alpha*x+y}
    val gemm_gold = (0::dim_M,0::dim_N){(i,j) =>
      Array.tabulate(dim_K){l => matrix_A(i,l)*matrix_B(l,j)}.reduce{_+_}
    }
    val gemv_gold = Array.tabulate(dim_M){i =>
      Array.tabulate(dim_K){l => matrix_A(i,l)*gemv_X_data(l)}.reduce{_+_}
    }
    val ger_gold = (0::dim_M, 0::dim_N){(i,j) => ger_X_data(i)*Y_data(j)}
    val scal_gold = X_data.map{_*alpha}
    val axpby_gold = X_data.zip(Y_data){case (x,y) => alpha*x+beta*y}

    // Collect cksums
    val margin = 0.25.to[T]
    val dot_cksum = abs(dot_res - dot_gold) < margin
    val axpy_cksum = axpy_res.zip(axpy_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val gemm_cksum = gemm_res.zip(gemm_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val gemv_cksum = gemv_res.zip(gemv_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val ger_cksum = ger_res.zip(ger_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val scal_cksum = scal_res.zip(scal_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val axpby_cksum = axpby_res.zip(axpby_gold){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksum = dot_cksum && axpy_cksum && gemm_cksum && gemv_cksum && ger_cksum && scal_cksum && axpby_cksum

    // Print results
    println("Dot Result:")
    println("  " + dot_res + " =?= " + dot_gold)
    println("Axpy Result:")
    printArray(axpy_res, "  Got")
    printArray(axpy_gold, "  Wanted")
    println("Gemm Result:")
    printMatrix(gemm_res, "  Got")
    printMatrix(gemm_gold, "  Wanted")
    println("Gemv Result:")
    printArray(gemv_res, "  Got")
    printArray(gemv_gold, "  Wanted")
    println("Ger Result:")
    printMatrix(ger_res, "  Got")
    printMatrix(ger_gold, "  Wanted")
    println("Scal Result:")
    printArray(scal_res, "  Got")
    printArray(scal_gold, "  Wanted")
    println("Axpby Result:")
    printArray(axpby_res, "  Got")
    printArray(axpby_gold, "  Wanted")
    assert(dot_cksum)
    assert(axpy_cksum)
    assert(gemm_cksum)
    assert(gemv_cksum)
    assert(ger_cksum)
    assert(scal_cksum)
    assert(axpby_cksum)
    assert(cksum)
    println("PASS: " + cksum + " (BasicBLAS)")

  }
}

package spatial.lib

import forge.tags._
import spatial.dsl._

object BLAS {

  @virtualize
  @api def Dot[T:Num](
    N: Int,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int
  ): T = {
    // Loop over whole vectors
    val out = Reduce(0.to[T])(N by 64/*tileSize*/ par 1/*outer_par*/){i =>
      // Compute elements left in this tile
      val elements = min(64/*tileSize*/, N - i)
      // Create onchip structures
      val x_tile = SRAM[T](64/*tileSize*/)
      val y_tile = SRAM[T](64/*tileSize*/)
      // Load local tiles
      x_tile load X(i::i+elements par 1/*load_par*/)
      y_tile load Y(i::i+elements par 1/*load_par*/)
      // Loop over elements in local tiles
      val inner_res = Reduce(Reg[T])(elements by 1 par 1/*inner_par*/){j =>
        x_tile(j) * y_tile(j)
      }{_+_}
      inner_res
    }{_+_}
    out.value
  }

  @virtualize
  @api def Axpy[T:Num](
    N: Int, alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int,
    res: DRAM1[T]
  ): Unit = {
    // Loop over whole vectors
    Foreach(N by 64/*tileSize*/ par 1/*outer_par*/){i =>
      // Compute elements left in this tile
      val elements = min(64/*tileSize*/, N - i)
      // Create onchip structures
      val x_tile = SRAM[T](64/*tileSize*/)
      val y_tile = SRAM[T](64/*tileSize*/)
      val z_tile = SRAM[T](64/*tileSize*/)
      // Load local tiles
      x_tile load X(i::i+elements par 1/*load_par*/)
      y_tile load Y(i::i+elements par 1/*load_par*/)
      // Loop over elements in local tiles
      Foreach(elements by 1 par 1/*inner_par*/){j =>
        z_tile(j) = alpha * x_tile(j) + y_tile(j)
      }
      // Store tile to DRAM
      res(i::i+elements par 1/*store_par*/) store z_tile
    }
  }

  @virtualize
  @api def Gemm[T:Num](
    M: Int, N: Int, K: Int,
    alpha: T,
    A: DRAM2[T], lda: Int,
    B: DRAM2[T], ldb: Int,
    beta: T,
    C: DRAM2[T], ldc: Int
  ): Unit = {
    Foreach(M by 64/*tileSizeM*/ par 1/*m_outer_par*/){i =>
      // Compute leftover dim
      val elements_m = min(64/*tileSizeM*/, M - i)
      Foreach(N by 64/*tileSizeN*/ par 1/*n_outer_par*/){j =>
        // Compute leftover dim
        val elements_n = min(64/*tileSizeN*/, N - j)
        // Create C tile for accumulating
        val c_tile = SRAM[T](64/*tileSizeM*/, 64/*tileSizeN*/)
        MemReduce(c_tile par 1/*c_reduce_par*/)(K by 64/*tileSizeK*/ par 1/*k_outer_par*/){l =>
          // Create local C tile
          val c_tile_local = SRAM[T](64/*tileSizeM*/, 64/*tileSizeN*/)
          // Compute leftover dim
          val elements_k = min(64/*tileSizeK*/, K - l)
          // Generate A and B tiles
          val a_tile = SRAM[T](64/*tileSizeM*/, 64/*tileSizeK*/)
          val b_tile = SRAM[T](64/*tileSizeK*/, 64/*tileSizeN*/)
          // Transfer tiles to sram
          Parallel{
            a_tile load A(i::i+elements_m, l::l+elements_k par 1/*load_par*/)
            b_tile load B(l::l+elements_k, j::j+elements_n par 1/*load_par*/)
          }
          Foreach(elements_m by 1 par 1/*m_inner_par*/){ii =>
            Foreach(elements_n by 1 par 1/*n_inner_par*/){jj =>
              c_tile_local(ii,jj) = Reduce(Reg[T])(elements_k by 1 par 1/*k_inner_par*/){ll =>
                a_tile(ii,ll) * b_tile(ll,jj)
              }{_+_}
            }
          }
          c_tile_local
        }{_+_}
        C(i::i+elements_m, j::j+elements_n par 1/*store_par*/) store c_tile
      }
    }
  }

  @virtualize
  @api def Gemv[T:Num](
    M: Int, N: Int,
    alpha: T,
    A: DRAM2[T], lda: Int,
    X: DRAM1[T], incX: Int,
    beta: T,
    Y: DRAM1[T], incY: Int
  ): Unit = {
    Foreach(M by 64/*tileSizeM*/ par 1/*m_outer_par*/){i =>
      // Compute leftover dim
      val elements_m = min(64/*tileSizeM*/, M - i)
      // Create Y tile
      val y_tile = SRAM[T](64/*tileSizeM*/)
      MemReduce(y_tile par 1/*y_reduce_par*/)(N by 64/*tileSizeN*/ par 1/*n_outer_par*/){j =>
        // Compute leftover dim
        val elements_n = min(64/*tileSizeN*/, N - j)
        // Create local Y tile for accumulating
        val y_tile_local = SRAM[T](64/*tileSizeM*/)
        // Create X tile
        val x_tile = SRAM[T](64/*tileSizeN*/)
        // Load vector tile
        x_tile load X(j::j+elements_n par 1/*load_par*/)
        // Create A tile
        val a_tile = SRAM[T](64/*tileSizeM*/, 64/*tileSizeN*/)
        // Load matrix tile
        a_tile load A(i::i+elements_m, j::j+elements_n par 1/*load_par*/)
        Foreach(elements_m by 1 par 1/*m_inner_par*/){ii =>
          y_tile_local(ii) = Reduce(Reg[T])(elements_n by 1 par 1/*n_inner_par*/){jj =>
            a_tile(ii,jj) * x_tile(jj)
          }{_+_}
        }
        y_tile_local
      }{_+_}
      Y(i::i+elements_m par 1/*store_par*/) store y_tile
    }
  }

  @virtualize
  @api def Ger[T:Num](
    M: Int, N: Int,
    alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T], incY: Int,
    A: DRAM2[T], lda: Int
  ): Unit = {
    Foreach(M by 16/*tileSizeM*/ par 1/*m_outer_par*/){i =>
      // Compute leftover dim
      val elements_m = min(16/*tileSizeM*/, M - i)
      // Create X tile
      val x_tile = SRAM[T](16/*tileSizeM*/)
      // Load x data into tile
      x_tile load X(i::i+elements_m)
      Foreach(N by 16/*tileSizeN*/ par 1/*n_outer_par*/){j =>
        // Compute leftover dim
        val elements_n = min(16/*tileSizeN*/, N - j)
        // Create Y and A tiles
        val y_tile = SRAM[T](16/*tileSizeN*/)
        val a_tile = SRAM[T](16/*tileSizeM*/, 16/*tileSizeN*/)
        // Load x data into tile
        y_tile load Y(j::j+elements_n)
        Foreach(elements_m by 1 par 1/*m_inner_par*/){ii =>
          Foreach(elements_n by 1 par 1/*n_inner_par*/){jj =>
            a_tile(ii,jj) = x_tile(ii) * y_tile(jj)
          }
        }
        A(i::i+elements_m, j::j+elements_n par 1/*store_par*/) store a_tile
      }
    }
  }

  @virtualize
  @api def Scal[T:Num](
    N: Int, alpha: T,
    X: DRAM1[T], incX: Int,
    Y: DRAM1[T]
  ): Unit = {
    // Loop over whole vectors
    Foreach(N by 64/*tileSize*/ par 1 /*outer_par*/){i =>
      // Compute elements left in this tile
      val elements = min(64/*tileSize*/, N - i)
      // Create onchip structures
      val x_tile = SRAM[T](64/*tileSize*/)
      val y_tile = SRAM[T](64/*tileSize*/)
      // Load local tiles
      x_tile load X(i::i+elements par 1 /*load_par*/)
      // Loop over elements in local tiles
      Foreach(elements by 1 par 1 /*inner_par*/){j =>
        y_tile(j) = alpha * x_tile(j)
      }
      // Store tile to DRAM
      Y(i::i+elements par 1 /*store_par*/) store y_tile
    }
  }

  @virtualize
  @api def Axpby[T:Num](
    N: Int,
    alpha: T,
    X: DRAM1[T], incX: Int,
    beta: T,
    Y: DRAM1[T], incY: Int,
    Z: DRAM1[T]
  ): Unit = {
    Scal[T](N, beta, Y, incY, Z)
    Axpy[T](N, alpha, X, incX, Z, incY, Z)
  }

}

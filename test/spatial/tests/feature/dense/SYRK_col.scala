package spatial.tests.feature.dense

import spatial.dsl._



/**
  * SYRK (blocked by columns)
  * -------------------------
  * C := C + AA' with N*K matrix A, updating only lower triangular part of symmetric N*N matrix C.
  */
@test class SYRK_col extends SpatialTest {
  override def runtimeArgs: Args = "64"
  type T = Int

  val full_K = 64
  val inner_N = 32
  val margin = 1

  def syrk_col(C: Array[T], A: Array[T], N: Int): Array[T] = {
    val full_N = ArgIn[Int]
    setArg(full_N, N)

    val OCC = DRAM[T](full_N, full_N)
    val OCA = DRAM[T](full_N, full_K)
    setMem(OCC, C)
    setMem(OCA, A)

    // Notations borrowed from Figure 5.3 of Pedram.
    Accel {
      val A1 = SRAM[T](inner_N, full_K)
      val A2_block = SRAM[T](inner_N, full_K) // A horizontal block of A2.
      val C_block = SRAM[T](inner_N, inner_N) // A block from C (either C11 or a block of C21).
      Sequential.Foreach(full_N by inner_N) { tile =>
        A1 load OCA(tile :: tile + inner_N, 0 :: full_K) // Provides data in both A1 and A1'.
        C_block load OCC(tile :: tile + inner_N, tile :: tile + inner_N) // C11.
        // C11 += A1 * A1' for elements on or below diagonal of C by inner product.
        Sequential.Foreach(inner_N by 1) { ii =>
          val row_len = ii + 1
          Sequential.Foreach(row_len by 1) { jj =>
            val prod = Reg[T]
            Reduce(prod)(full_K by 1) { k => A1(ii, k) * A1(jj, k) }{_+_}
            C_block(ii, jj) = C_block(ii, jj) + prod
          }
        }
        OCC(tile :: tile + inner_N, tile :: tile + inner_N) store C_block

        // Update C21 by inner_N*inner_N blocks.
        val C21_height = full_N.value - tile - inner_N.to[Int]
        Sequential.Foreach(C21_height by inner_N) { r_offset =>
          val r = tile + inner_N + r_offset // Block of C21 starting at r.
          C_block load OCC(r :: r + inner_N, tile :: tile + inner_N)
          A2_block load OCA(r :: r + inner_N, 0 :: full_K)

          // C21_block += A2_block * A1' by inner product.
          Sequential.Foreach(inner_N by 1, inner_N by 1) { (ii, jj) =>
            val prod = Reg[T]
            Reduce(prod)(full_K by 1) { k => A2_block(ii, k) * A1(jj, k) }{_+_}
            C_block(ii, jj) = C_block(ii, jj) + prod.value
          }

          OCC(r :: r + inner_N, tile :: tile + inner_N) store C_block
        }
      }
    }

    getMem(OCC)
  }


  def main(args: Array[String]): Unit = {

    val N = args(0).to[Int]
    /* TODO(zhangwen): what initial value to give? */
    val C = Array.tabulate(N) { i => Array.tabulate(N) { j => 0.to[T] } }
    //   if (j > i) 0
    //   else if (j == i) 1 random[T](10)
    //   else 1 /*random[T](0.5)*/
    // }}
    val A = Array.fill(N) {
      Array.fill(full_K) {
        1.to[T] /*random[T](1)}*/
      }
    }

    val result = syrk_col(C.flatten, A.flatten, N)

    val AAT = Array.tabulate(N) { i => // A*A', with zeros above diagonal.
      val A_row_i = A(i)
      Array.tabulate(N) { j =>
        val A_row_j = A(j)

        if (j > i) 0.to[T]
        else A_row_i.zip(A_row_j){_*_}.reduce{_+_}
      }
    }
    val C_check = C.flatten.zip(AAT.flatten) { (a, b) => a + b } // C + A*A'

    val C_computed = Array.tabulate(N * N) { i => result(i) }
    val cksum = C_computed.zip(C_check) { (a, b) => a > b - margin && a < b + margin }.reduce{_&&_}
    println("PASS: " + cksum)
    assert(cksum)
  }
}


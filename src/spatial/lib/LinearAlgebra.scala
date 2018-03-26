package spatial.lib

import argon._
import spatial.lang._

trait LinearAlgebra {

  def transpose[T:Num](
    Y:    SRAM2[T],
    perm: Seq[Int],
    A:    SRAM2[T]
  )(implicit state: State): Unit = {
    val M = A.rows
    val N = A.cols
    val MP: I32 = 1 (1 -> 32)
    val NP: I32 = 1 (1 -> 32)

    def getA(i: I32, j: I32): T = perm match {
      case Seq(0,1) => A(i,j)
      case Seq(1,0) => A(j,i)
      case _ => throw new Exception("Invalid permutation indices for 2D transpose: " + perm)
    }

    Foreach(M par MP, N par NP){(i,j) => Y(i,j) = getA(i,j) }
  }

  /** Broadcast scalar C version */
  def gemm[T:Num](
    Y:      SRAM2[T],
    A:      SRAM2[T],
    B:      SRAM2[T],
    C:      T,
    transA: Boolean,
    transB: Boolean,
    alpha:  T,
    beta:   T,
    mp:     Option[I32] = None,
    kp:     Option[I32] = None,
    np:     Option[I32] = None
  )(implicit state: State): Unit = {
    val M = A.rows
    val P = A.cols
    val N = B.cols
    val MP: I32 = mp.getOrElse{ 1 (1 -> 32) }
    val NP: I32 = np.getOrElse{ 1 (1 -> 16) }

    val MT: I32 = 16 (1 -> 32)
    val NT: I32 = 16 (1 -> 32)

    Foreach(M by MT par MP, N by NT par NP){(i,j) =>
      BlackBox.GEMM(Y, A, B, C, alpha, beta, i, j, P, MT, NT)
    }
    /*def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    Foreach(M par MP, N par NP){(i,j) =>
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      Y(i,j) = prod.value*liftA(alpha) + cb
    }*/
  }

}

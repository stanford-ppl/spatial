package spatial.lib

import argon._
import forge.tags._
import spatial.dsl._

trait LinearAlgebra {
  @virtualize
  def matmult[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    mp: scala.Int = 1,
    kp: scala.Int = 1,
    np: scala.Int = 1
  )(implicit state: State): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = mp (1 -> 32)
    val KP: Int = kp (1 -> 16)
    val NP: Int = np (1 -> 16)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)
    def storeY(i: Int, j: Int, x: T): Unit = if (transY) Y(j,i) = x else Y(i,j) = x

    Foreach(M par MP, N par NP){(i,j) =>
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      storeY(i, j, prod.value)
    }
  }

  sealed abstract class BoxedC[T]
  case class CScalar[T](c: T) extends BoxedC[T]
  case class CVector[T](c: SRAM1[T]) extends BoxedC[T]
  case class CMatrix[T](c: SRAM2[T]) extends BoxedC[T]

  /** Broadcast scalar C version **/
  @virtualize
  def gemm[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: T,
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: T,
    beta: T,
    mp: scala.Int,
    kp: scala.Int,
    np: scala.Int
  )(implicit state: State): Unit = {
    gemm_generalized(Y,A,B,CScalar(C),transA,transB,transY,sumY,alpha,beta,mp,kp,np)
  }


  /** Broadcast array C version **/
  @virtualize
  def gemm[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],    // M x K
    B: SRAM2[T],    // K x N
    C: SRAM1[T],    // 1 x N
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: T,
    beta: T,
    mp: scala.Int,
    kp: scala.Int,
    np: scala.Int
  )(implicit state: State): Unit = {
    gemm_generalized(Y,A,B,CVector(C),transA,transB,transY,sumY,alpha,beta,mp,kp,np)
  }

  /** Matrix C version **/
  @virtualize
  def gemm[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: SRAM2[T],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: T,
    beta: T,
    mp: scala.Int,
    kp: scala.Int,
    np: scala.Int
  )(implicit state: State): Unit = {
    gemm_generalized(Y,A,B,CMatrix(C),transA,transB,transY,sumY,alpha,beta,mp,kp,np)
  }


  /** Unified general form of GEMM - Make modifications to this one! */
  // TODO: The nested Either is gross but Scala is freaking out asking for implicit state if
  // you instead try to go with the sealed wrapper class approach
  @virtualize
  private def gemm_generalized[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: BoxedC[T],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: T,
    beta: T,
    mp: scala.Int,
    kp: scala.Int,
    np: scala.Int
  )(implicit state: State): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = mp (1 -> 32)
    val KP: Int = kp (1 -> 16)
    val NP: Int = np (1 -> 16)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)
    def getC(i: Int, j: Int): T = C match {
      case CScalar(value)  => value
      case CVector(vector) => vector(j)
      case CMatrix(matrix) => matrix(i,j)
    }
    def storeY(i: Int, j: Int, v: T): Unit = (transY, sumY) match {
      case (true, true)   => Y(j,i) = Y(j,i) + v
      case (true, false)  => Y(j,i) = v
      case (false, true)  => Y(i,j) = Y(i,j) + v
      case (false, false) => Y(i,j) = v
    }

    def isRowVector(x: SRAM2[_], trans: scala.Boolean): Boolean = if (trans) x.cols === 1.to[Int] else x.rows === 1.to[Int]
    def isColVector(x: SRAM2[_], trans: scala.Boolean): Boolean = if (trans) x.rows === 1.to[Int] else x.cols === 1.to[Int]

    def isOuterProduct: scala.Boolean = {
      val cond = (isRowVector(A,transA) && isColVector(B,transB)) ||
        (isColVector(A,transA) && isRowVector(B,transB))

      cond match {
        case Literal(true) => true
        case _ => false
      }
    }
    def isInnerProduct: scala.Boolean = {
      val cond = (isRowVector(A,transA) && isRowVector(B,transB)) ||
        (isColVector(A,transA) && isColVector(B,transB))
      cond match {
        case Literal(true) => true
        case _ => false
      }
    }

    if (isOuterProduct) {
      /** On-chip outer product */
      Foreach(M par MP, N par NP) { (i, j) =>
        val out = getA(i, 0.to[Int]) * getB(0.to[Int], j) * alpha + getC(i,j) * beta
        storeY(i, j, out)
      }
    }
    else if (isInnerProduct) {
      /** On-chip inner (dot) product */
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(0,k) * getB(k,0) }{_+_}
      val out = prod.value*alpha + getC(0,0)*beta
      storeY(0,0, out)
    }
    else {
      /** On-chip GEMM */

      // // Original:
      // Stream.Foreach(M par MP, N par NP){(i,j) =>
      //   val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      //   val out = prod.value*alpha + getC(i,j)*beta
      //   storeY(i,j, out)
      // }

      // Pseudo-Proposed:
      Stream.Foreach(M par MP, N par NP){(i,j) =>
        val prod = FIFO[T](2)
        Pipe{prod.enq(Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_})}
        Pipe{
          val out = prod.deq*alpha + getC(i,j)*beta
          storeY(i,j, out)
        }
      }
      
      // // Proposed in issue #44:
      // Stream.Foreach(M par MP, N par NP){(i,j) =>
      //   val prod = FIFO[T](2)
      //   Reduce(prod)(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      //   val out = prod.deq*alpha + getC(i,j)*beta
      //   storeY(i,j, out)
      // }
    }
  }

  /** Broadcast scalar C version **/
  @virtualize
  def gemm_fpga_fix[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: T,
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: T,
    beta: T,
    mp: scala.Int = 1,
    kp: scala.Int = 1,
    np: scala.Int = 1
  )(implicit state: State): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = mp (1 -> 32)
    val KP: Int = kp (1 -> 16)
    val NP: Int = np (1 -> 16)
    val cb  = C*beta

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    if (sumY) {
      MemReduce(Y)(M par MP, N par NP){(i,j) =>
        val tmp = SRAM[T](Y.rows, Y.cols)
        val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
        val out = prod.value*alpha + cb
        if (transY)
          tmp(j,i) = out
        else
          tmp(i,j) = out
        tmp
      }{_+_}
    } else {
      Foreach(M par MP, N par NP){(i,j) =>
        val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
        val out = prod.value*alpha + cb
        if (transY)
          Y(j,i) = out
        else
          Y(i,j) = out
      }
    }
  }

  @virtualize
  def maxpool2d[T:Num](
    Y: SRAM4[T],
    X: SRAM4[T],
    kernel: (Int,Int),
    pad: (Int,Int,Int,Int),
    stride: (Int,Int)
  )(implicit state: State): Unit = {
    val D0 = Y.dim0
    val D1 = Y.dim1
    val D2 = Y.dim2
    val D3 = Y.dim3
    val D0P: Int = 1 (1 -> 16)
    val D1P: Int = 1 (1 -> 16)
    val D2P: Int = 1 (1 -> 16)
    val D3P: Int = 1 (1 -> 16)

    val I = kernel._1
    val J = kernel._2

    Foreach( D0 par D0P, D1 par D1P, D2 par D2P, D3 par D3P ){ (d) =>

      val max_ = Reduce( Reg[T](0.to[T]) )( I by 1, J by 1 ){ (i,j) =>
        val xi = stride._1*d(2) - pad._1 + i
        val xj = stride._2*d(3) - pad._2 + j
        val v = if ( xi.to[Int] < 0 || xi.to[Int] >= X.dim2 || xj.to[Int] < 0 || xj.to[Int] >= X.dim3 ) 0.to[T] else X(d(0),d(1),xi,xj)
        v
      }{ (a,b) => max(a,b) }
      Y(d(0),d(1),d(2),d(3)) = max_
    }
  }

  @virtualize
  def averagepool2d[T:Type:Num](
    Y: SRAM4[T],
    X: SRAM4[T],
    kernel: (Int,Int),
    pad: (Int,Int,Int,Int),
    stride: (Int,Int)
  )(implicit state: State): Unit = {
    val D0 = Y.dim0
    val D1 = Y.dim1
    val D2 = Y.dim2
    val D3 = Y.dim3
    val D0P: Int = 1 (1 -> 16)
    val D1P: Int = 1 (1 -> 16)
    val D2P: Int = 1 (1 -> 16)
    val D3P: Int = 1 (1 -> 16)

    val I = kernel._1
    val J = kernel._2

    Foreach( D0 par D0P, D1 par D1P, D2 par D2P, D3 par D3P ){ (d) =>

      val sum = Reduce( Reg[T](0.to[T]) )( I by 1, J by 1 ){ (i,j) =>
        val xi = stride._1*d(2) - pad._1 + i
        val xj = stride._2*d(3) - pad._2 + j
        val v = if ( xi.to[Int] < 0 || xi.to[Int] >= X.dim2 || xj.to[Int] < 0 || xj.to[Int] >= X.dim3 ) 0.to[T] else X(d(0),d(1),xi,xj)
        v
      }{ _+_ }
      Y(d(0),d(1),d(2),d(3)) = sum.value /( I.to[T] * J.to[T] )
    }
  }

  @virtualize
  def conv2d[T:Num](
    Y: SRAM4[T],          // Img_out: Nout x Cout x Hout x Wout
    X: SRAM4[T],          // Img_in: Nin x Cin x Hin x Hin
    K: SRAM4[T],          // Weights: 1 x Cin x Kh x Kw
    pad: (Int,Int),
    stride: (Int,Int)    // Convolution stride
  )(implicit state: State): Unit = {
    val D0 = Y.dim0
    val D1 = Y.dim1
    val D2 = Y.dim2
    val D3 = Y.dim3
    val D0P: Int = 1 (1 -> 16)
    val D1P: Int = 1 (1 -> 16)
    val D2P: Int = 1 (1 -> 16)
    val D3P: Int = 1 (1 -> 16)

    val A = K.dim1
    val B = K.dim2
    val C = K.dim3

    // Assume N=1
    Foreach( D0 par D0P, D1 par D1P, D2 par D2P, D3 par D3P ){ (d) =>

      val conv = Reduce( Reg[T](0.to[T]) )( A by 1, B by 1, C by 1 ){ (a,b,c) =>
        val xb = stride._1*d(2) - pad._1 + b
        val xc = stride._2*d(3) - pad._2 + c
        val v = if ( xb.to[Int] < 0 || xb.to[Int] >= X.dim2 || xc.to[Int] < 0 || xc.to[Int] >= X.dim3 ) 0.to[T] else X(d(0),a,xb,xc)
        v * K(d(1),a,b,c)
        //println( "[" + d(2) + "," + d(3) + "] :"
        //  + " (" + i + "," + j + ") * (" + xi + "," + xj + ") = "
        //  + v + " * " + K(d(0),d(1),i,j) + " = " + tot )
      }{ _+_ }
      //println( "conv = " + conv )
      Y(d(0),d(1),d(2),d(3)) = conv
    }
  }

  @virtualize
  def batchNorm2d[T:Num](
    Y: SRAM4[T],          // out: NCHW
    X: SRAM4[T],          // in:  NCHW
    scale: SRAM1[T],      // in:  C
    B: SRAM1[T],          // in:  C
    epsilon: Float
  )(implicit state: State): Unit = {
    val D0 = Y.dim0
    val D1 = Y.dim1
    val D2 = Y.dim2
    val D3 = Y.dim3
    val D0P: Int = 1 (1 -> 16)
    val D1P: Int = 1 (1 -> 16)
    val D2P: Int = 1 (1 -> 16)
    val D3P: Int = 1 (1 -> 16)

    val elems = D0 * D2 * D3

    Foreach( D1 par D1P ){ (d1) =>
      val mean_sum = Reduce( Reg[T](0.to[T]) )( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        X(d0,d1,d2,d3)
      }{ _+_ }
      val mean = mean_sum.value / elems.to[T]
      val var_sum = Reduce( Reg[T](0.to[T]) )( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        pow( X(d0,d1,d2,d3) - mean, 2 ).to[T]
      }{ _+_ }
      val vari = var_sum.value / elems.to[T]
      val divisor = sqrt( vari.to[Float] + epsilon ).to[T]
      Foreach( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        Y(d0,d1,d2,d3) = ( X(d0,d1,d2,d3) - mean )/( divisor ) * scale(d1) + B(d1)
      }
    }
  }

  @virtualize
  def batchNorm2d[T:Num](
    Y: SRAM4[T],          // out: NCHW
    X: SRAM4[T],          // in:  NCHW
    scale: T,      // in:  C
    B: T,          // in:  C
    epsilon: Float
  )(implicit state: State): Unit = {
    val D0 = Y.dim0
    val D1 = Y.dim1
    val D2 = Y.dim2
    val D3 = Y.dim3
    val D0P: Int = 1 (1 -> 16)
    val D1P: Int = 1 (1 -> 16)
    val D2P: Int = 1 (1 -> 16)
    val D3P: Int = 1 (1 -> 16)

    val elems = D0 * D2 * D3

    Foreach( D1 par D1P ){ (d1) =>
      val mean_sum = Reduce( Reg[T](0.to[T]) )( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        X(d0,d1,d2,d3)
      }{ _+_ }
      val mean = mean_sum.value / elems.to[T]
      val var_sum = Reduce( Reg[T](0.to[T]) )( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        pow( X(d0,d1,d2,d3) - mean, 2 ).to[T]
      }{ _+_ }
      val vari = var_sum.value / elems.to[T]
      val divisor = sqrt( vari.to[Float] + epsilon ).to[T]
      Foreach( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        Y(d0,d1,d2,d3) = ( X(d0,d1,d2,d3) - mean )/( divisor ) * scale + B
      }
    }
  }
}

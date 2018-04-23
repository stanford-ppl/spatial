package spatial.lib

import forge.tags._
import spatial.dsl._

trait LinearAlgebra {

  @virtualize
  @api def matmult[T:Num](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    mp: scala.Int = 1,
    kp: scala.Int = 1,
    np: scala.Int = 1
  ): Unit = {
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

  @virtualize
  @api def gemm[T:Num,A,B](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: SRAM2[T],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: A,
    beta: B
  )(implicit castA: Cast[A,T], castB: Cast[B,T]): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = 1 (1 -> 32)
    val KP: Int = 1 (1 -> 16)
    val NP: Int = 1 (1 -> 16)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    Foreach(M par MP, N par NP){(i,j) =>
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      val out = prod.value*castA(alpha) + C(i,j)*castB(beta)
      if (transY)
        if (sumY)
          Y(j,i) = Y(j,i) + out
        else
          Y(j,i) = out
      else
        if (sumY)
          Y(i,j) = Y(i,j) + out
        else
          Y(i,j) = out
    }
  }

  /** Broadcast scalar C version **/
  @virtualize
  @api def gemm[T:Num, A, B](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: T,
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: A,
    beta: B
  )(implicit liftA: Cast[A,T], liftB: Cast[B,T]): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = 1 (1 -> 32)
    val KP: Int = 1 (1 -> 16)
    val NP: Int = 1 (1 -> 16)
    val cb  = C*liftB(beta)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    Foreach(M par MP, N par NP){(i,j) =>
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      val out = prod.value*liftA(alpha) + cb
      if (transY)
        if (sumY)
          Y(j,i) = Y(j,i) + out
        else
          Y(j,i) = out
      else
        if (sumY)
          Y(i,j) = Y(i,j) + out
        else
          Y(i,j) = out
    }
  }

  /** Broadcast scalar C version **/
  @virtualize
  @api def gemm_fpga_fix[T:Num, A, B](
    Y: SRAM2[T],
    A: SRAM2[T],
    B: SRAM2[T],
    C: T,
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: A,
    beta: B
  )(implicit liftA: Cast[A,T], liftB: Cast[B,T]): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = 1 (1 -> 32)
    val KP: Int = 1 (1 -> 16)
    val NP: Int = 1 (1 -> 16)
    val cb  = C*liftB(beta)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    if (sumY) {
      MemReduce(Y)(M par MP, N par NP){(i,j) =>
        val tmp = SRAM[T](Y.rows, Y.cols)
        val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
        val out = prod.value*liftA(alpha) + cb
        if (transY)
          tmp(j,i) = out
        else
          tmp(i,j) = out
        tmp
      }{_+_}
    } else {
      Foreach(M par MP, N par NP){(i,j) =>
        val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
        val out = prod.value*liftA(alpha) + cb
        if (transY)
            Y(j,i) = out
        else
            Y(i,j) = out
      }
    }
  }

  /** Broadcast array C version **/
  @virtualize
  @api def gemm[T:Num, A, B](
    Y: SRAM2[T],
    A: SRAM2[T],    // M x K
    B: SRAM2[T],    // K x N
    C: SRAM1[T],    // M x N
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean,
    sumY: scala.Boolean,
    alpha: A,
    beta: B
  )(implicit liftA: Cast[A,T], liftB: Cast[B,T]): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = 1 (1 -> 32)
    val KP: Int = 1 (1 -> 16)
    val NP: Int = 1 (1 -> 16)

    def getA(i: Int, j: Int): T = if (transA) A(j,i) else A(i,j)
    def getB(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)
    def getC(i: Int, j: Int): T = if (transB) B(j,i) else B(i,j)

    Foreach(M par MP, N par NP){(i,j) =>
      val prod = Reduce(Reg[T])(K by 1 par KP){k => getA(i,k) * getB(k,j) }{_+_}
      val out = prod.value*liftA(alpha) + C(j)*liftB(beta)
      if (transY)
        if (sumY)
          Y(j,i) = Y(j,i) + out
        else
          Y(j,i) = out
      else
        if (sumY)
          Y(i,j) = Y(i,j) + out
        else
          Y(i,j) = out
    }
  }

  @virtualize
  @api def maxpool2d[T:Num](
    Y: SRAM4[T],
    X: SRAM4[T],
    kernel: (Int,Int),
    pad: (Int,Int),
    stride: (Int,Int)
  ): Unit = {
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
  @api def averagepool2d[T:Num](
    Y: SRAM4[T],
    X: SRAM4[T],
    kernel: (Int,Int),
    pad: (Int,Int),
    stride: (Int,Int)
  ): Unit = {
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
  @api def conv2d[T:Num](
    Y: SRAM4[T],          // Img_out: Nout x Cout x Hout x Wout
    X: SRAM4[T],          // Img_in: Nin x Cin x Hin x Hin
    K: SRAM4[T],          // Weights: 1 x Cin x Kh x Kw
    pad: (Int,Int),
    stride: (Int,Int)    // Convolution stride
  ): Unit = {
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
  @api def batchNorm2d[T:Num](
    Y: SRAM4[T],          // out: NCHW
    X: SRAM4[T],          // in:  NCHW
    scale: SRAM1[T],      // in:  C
    B: SRAM1[T],          // in:  C
    epsilon: Float
  ): Unit = {
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
      }{_+_}
      val mean = mean_sum.value / elems.to[T]
      val var_sum = Reduce( Reg[T](0.to[T]) )( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        pow( X(d0,d1,d2,d3) - mean, 2 ).to[T]
      }{_+_}
      val vari = var_sum.value / elems.to[T]
      val divisor = sqrt( vari.to[Float] + epsilon ).to[T]

      Foreach( D0 par D0P, D2 par D2P, D3 par D3P ){ (d0,d2,d3) =>
        Y(d0,d1,d2,d3) = ( X(d0,d1,d2,d3) - mean )/( divisor ) * scale(d1) + B(d1)
      }
    }
  }

  @virtualize
  @api def batchNorm2d[T:Num](
    Y: SRAM4[T],          // out: NCHW
    X: SRAM4[T],          // in:  NCHW
    scale: T,      // in:  C
    B: T,          // in:  C
    epsilon: Float
  ): Unit = {
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

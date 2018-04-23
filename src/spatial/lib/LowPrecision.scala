package spatial.lib

import forge.tags._
import spatial.dsl._

trait LowPrecision {

  @virtualize
  @api def ConvertTo8Bit(
    Y: DRAM1[Byte],   // Output List of weights converted into integers
    SF: Reg[Float],   // Output Scaling Factor
    X: DRAM1[Float],  // Input List of weights ranging [-1 to 1, floating point numbers]
    size: Int
  ): Unit = {

    val sram_in = SRAM[Float](size)
    val sram_out = SRAM[Byte](size)
    val maxo = Reg[Float]
    val delta = Reg[Float]

    Reduce(maxo)(X.length by size) { ii =>
      sram_in load X(ii :: ii + size)
      val maxi = Reg[Float]
      Reduce(maxi)(0 until size) { jj =>
        abs(sram_in(jj))
      }{ (a, b) => max(a, b) }
    }{ (a, b) => max(a, b) }

    Pipe {
      delta := 2.to[Float] * (maxo) / 127.to[Float] // FIXME: Statistical Rounding not done. Instead we are doing Float2Fix
    }

    Foreach(X.length by size) { ii =>
      sram_in load X(ii :: ii + size)
      Foreach(0 until size) { jj =>
        sram_out(jj) = (sram_in(jj) / delta).to[Byte]
      }
      Y(ii :: ii + size) store sram_out
    }

    Pipe {
      SF := delta
    }
  }


  @virtualize
  @api def quantize[T:Num](
    parent: Label,
    Y: SRAM2[T], // Output List of weights converted into integers
    SF: Reg[Float], // Output Scaling Factor
    X: SRAM2[Float], // Input List of weights ranging [-1 to 1, floating point numbers]
    factor: scala.Int, // Quantization Factor
    precision: scala.Int //
  ): Unit = {
    val sat_neg = (-scala.math.pow(2, (precision - factor - 1))).to[Int]
    val sat_pos = (scala.math.pow(2, (precision - factor - 1)) - 1).to[Int]
    val maxsram = Reg[Float]
    val delta = Reg[Float]

    Named(s"${parent}_Reduce").Reduce(maxsram)(X.rows by 1, X.cols by 1) { (i, j) =>
      abs(X(i, j))
    } { (a, b) => max(a, b) }

    Named(s"${parent}_Delta").Pipe {
      delta := (maxsram) / scala.math.pow(2, (precision - factor - 1))
    }

    Named(s"${parent}_Myforeach").Foreach(X.rows by 1, X.cols by 1) { (i, j) =>
      val quant_out = (X(i, j) / delta).to[Int]
      Y(i, j) = mux(quant_out > sat_pos, sat_pos.to[T], mux(quant_out < sat_neg, sat_neg.to[T], quant_out.to[T]))
    }

    Pipe {
      SF := delta.value
    }
  }

  @virtualize
  @api def quantize[T:Num](
    parent: Label,
    Y: SRAM1[T], // Output List of weights converted into integers
    SF: Reg[Float], // Output Scaling Factor
    X: SRAM1[Float], // Input List of weights ranging [-1 to 1, floating point numbers]
    factor: scala.Int, // Quantization Factor
    precision: scala.Int
  ): Unit = {
    val sat_neg = (-scala.math.pow(2, (precision - factor - 1))).to[Int]
    val sat_pos = (scala.math.pow(2, (precision - factor - 1)) - 1).to[Int]
    val maxsram = Reg[Float]
    val delta = Reg[Float]

    Named(s"${parent}_Reduce").Reduce(maxsram)(X.length by 1) { i =>
      abs(X(i))
    } { (a, b) => max(a, b) }

    Named(s"${parent}_Delta").Pipe {
      delta := ((maxsram) / scala.math.pow(2, (precision - factor - 1)))
    }

    Named(s"${parent}_Myforeach").Foreach(X.length by 1) { i =>
      val quant_out = (X(i) / delta).to[Int]
      Y(i) = mux(quant_out > sat_pos, sat_pos.to[T], mux(quant_out < sat_neg, sat_neg.to[T], quant_out.to[T]))
    }
    Pipe {
      SF := delta.value
    }
  }

  @virtualize
  @api def mmlp[IT:Num, OT:Num](
    parent: Label,
    Y: SRAM2[OT],
    YSF: Reg[Float],
    A: SRAM2[IT],
    ASF: Reg[Float],
    B: SRAM2[IT],
    BSF: Reg[Float],
    transA: scala.Boolean,
    transB: scala.Boolean,
    transY: scala.Boolean
  ): Unit = {
    val M = if (transA) A.cols else A.rows
    val K = if (transA) A.rows else A.cols
    val N = if (transB) B.rows else B.cols
    val MP: Int = 1 (1 -> 32)
    val KP: Int = 1 (1 -> 16)
    val NP: Int = 1 (1 -> 16)

    def getA(i: Int, j: Int): IT = if (transA) A(j, i) else A(i, j)
    def getB(i: Int, j: Int): IT = if (transB) B(j, i) else B(i, j)

    Named(s"${parent}_Myforeach").Foreach(M par MP, N par NP) { (i, j) =>
      val prod = Reduce(Reg[OT])(K by 1 par KP) { k => getA(i, k).to[OT] * getB(k, j).to[OT] } {
        _ + _
      }
      if (transY)
        Y(j, i) = prod.value
      else
        Y(i, j) = prod.value
    }

    Pipe {
      YSF := ASF.value * BSF.value
    }
  }

  //   @virtualize
  //   def addlp[T1:Type:Num,T2:Type:Num,OT:Type:Num] (
  //    parent: Label,
  //       Y: SRAM2[OT],   // Output List of weights converted into integers
  //     YSF: Reg[Float],   // Output Scaling Factor
  //      X1: SRAM2[T1],   // Input1 List of weights
  //    X1SF: Reg[Float],   // Input1 Scaling Factor
  //      X2: SRAM2[T2],   // Input2 List of weights
  //    X2SF: Reg[Float],   // Input2 Scaling Factor
  // precision: scala.Int,
  // factor: scala.Int
  //   )(implicit state: State): Unit = {
  //   val sat_neg =  (-scala.math.pow(2,(precision-factor-1))).to[Int]
  //   val sat_pos =  (scala.math.pow(2,(precision-factor-1))-1).to[Int]
  //         Named(s"${parent}_Myforeach").Foreach(X1.rows by 1, X1.cols by 1) { (i,j) =>
  //     val X2LP  = (X2(i,j).to[Float] * X2SF.value / X1SF.value).to[Int]
  //     val add_out = (X2LP + X1(i,j).to[Int])
  //     Y(i,j) = mux(add_out>sat_pos, sat_pos.to[OT], mux(add_out < sat_neg, sat_neg.to[OT],add_out.to[OT]))
  //         }
  //   Pipe {
  //        YSF := X1SF.value
  //   }
  //   }

  //  @virtualize
  //  def addlp (
  //   parent: Label,
  //      Y: SRAM2[Byte],   // Output List of weights converted into integers
  //    YSF: Reg[Float],   // Output Scaling Factor
  //     X1: SRAM2[Byte],   // Input1 List of weights
  //   X1SF: Reg[Float],   // Input1 Scaling Factor
  //     X2: SRAM1[Byte],   // Input2 List of weights (will be broadcast)
  //   X2SF: Reg[Float],   // Input2 Scaling Factor
  //   transX2 : scala.Boolean // Transpose Input2
  //  )(implicit state: State): Unit = {
  //        def getX2(i: Int, j: Int): Byte = if (transX2) X2(j) else X2(i)
  //
  //        Named(s"${parent}_Myforeach").Foreach(X1.rows by 1, X1.cols by 1) { (i,j) =>
  //    val X2LP  = (getX2(i,j).to[Float] * X2SF.value / X1SF.value).to[Byte]
  //    val X1pX2 = (X2LP + X1(i,j)).to[Int]
  //    val SATY  =  mux(X1pX2>127, 127.to[Byte], mux(X1pX2 < -128, -128.to[Byte],X1pX2.to[Byte]))  //FIXME: This can be replaced with saturating math library
  //    Y(i,j) = SATY
  //        }
  //  Pipe {
  //       YSF := X1SF.value
  //  }
  //  }

  @virtualize
  @api def addlp[T1:Num, T2:Num, OT:Num](
    parent: Label,
    Y: SRAM2[OT], // Output List of weights converted into integers
    YSF: Reg[Float], // Output Scaling Factor
    X1: SRAM2[T1], // Input1 List of weights
    X1SF: Reg[Float], // Input1 Scaling Factor
    X2: SRAM1[T2], // Input2 List of weights (will be broadcast)
    X2SF: Reg[Float], // Input2 Scaling Factor
    transX2: scala.Boolean, // Transpose Input2
    precision: scala.Int,
    factor: scala.Int
  ): Unit = {
    val sat_neg = (-scala.math.pow(2, (precision - factor - 1))).to[Int]
    val sat_pos = (scala.math.pow(2, (precision - factor - 1)) - 1).to[Int]

    def getX2(i: Int, j: Int): Int = if (transX2) X2(j).to[Int] else X2(i).to[Int]

    Named(s"${parent}_Myforeach").Foreach(X1.rows by 1, X1.cols by 1) { (i, j) =>
      val X2LP = (getX2(i, j).to[Float] * X2SF.value / X1SF.value).to[Int]
      val add_out = (X2LP + X1(i, j).to[Int])
      Y(i, j) = mux(add_out > sat_pos, sat_pos.to[OT], mux(add_out < sat_neg, sat_neg.to[OT], add_out.to[OT]))
    }
    Pipe {
      YSF := X1SF.value
    }
  }

  @virtualize
  @api def dequant[T:Num](
    parent: Label,
    Y: SRAM2[Float], // Output List of weights converted into integers
    SF: Reg[Float], // Output Scaling Factor
    X: SRAM2[T] // Input List of weights ranging [-1 to 1, floating point numbers]
  ): Unit = {
    Named(s"${parent}_Myforeach").Foreach(X.rows by 1, X.cols by 1) { (i, j) =>
      Y(i, j) = X(i, j).to[Float] * SF.value
    }
  }

  @virtualize
  @api def ConvertTo8Bit_Buggy( // SF gets 0 so output srams get INF. Control structure has a problem in HW.
    Y: DRAM1[Byte], // Output List of weights converted into integers
    SF: Reg[Float], // Output Scaling Factor
    X: DRAM1[Float], // Input List of weights ranging [-1 to 1, floating point numbers]
    size: Int
  ): Unit = {

    val sram_in = SRAM[Float](size)
    val sram_out = SRAM[Byte](size)
    val maxo = Reg[Float]

    Reduce(maxo)(X.length by size) { ii =>
      sram_in load X(ii :: ii + size)
      val maxi = Reg[Float]
      Reduce(maxi)(0 until size) { jj =>
        abs(sram_in(jj))
      } { (a, b) => max(a, b) }
    } { (a, b) => max(a, b) }

    Pipe {
      SF := 2.to[Float] * (maxo) / 127.to[Float] // FIXME: Statistical Rounding not done. Instead we are doing Float2Fix
    }

    Foreach(X.length by size) { ii =>
      sram_in load X(ii :: ii + size)
      Foreach(0 until size) { jj =>
        sram_out(jj) = (sram_in(jj) / SF).to[Byte]
      }
      Y(ii :: ii + size) store sram_out
    }
  }

  //  @virtualize
  //  def AddLP (
  //      Y: DRAM1[Byte],   // Output List of weights converted into integers
  //    YSF: Reg[Float],   // Output Scaling Factor
  //     X1: DRAM1[Byte],   // Input1 List of weights
  //   X1SF: Reg[Float],   // Input1 Scaling Factor
  //     X2: DRAM1[Byte],   // Input2 List of weights
  //   X2SF: Reg[Float],   // Input2 Scaling Factor
  //   size: Int
  //  )(implicit state: State): Unit = {
  //
  //  val sram_X1   = SRAM[Byte](size)
  //  val sram_X2   = SRAM[Byte](size)
  //  val sram_Y   = SRAM[Byte](size)
  //
  //  Foreach(X1.length by size) { ii =>
  //    Parallel{
  //            sram_X1 load X1(ii::ii+size)
  //            sram_X2 load X2(ii::ii+size)
  //    }
  //    Foreach(0 until size) { jj =>
  //      val convert = ((sram_X2(jj).to[Float] * X2SF) / X1SF).to[Byte]
  //      val add_result = (convert + sram_X1(jj)).to[Int]
  //      val add_result_final = mux(add_result>127, 127.to[Byte], mux(add_result < -128, -128.to[Byte],add_result.to[Byte]))  //FIXME: This can be replaced with saturating math library
  //      sram_Y(jj) = add_result_final
  //    }
  //    Y(ii::ii+size) store sram_Y
  //  }
  //  }

  @virtualize
  @api def testmem(
    Y: DRAM1[Float], // Output List
    X: DRAM1[Float], // Input List
    size: Int
  ): Unit = {

    val sram_in = SRAM[Float](size)
    val sram_out = SRAM[Float](size)

    Foreach(X.length by size) { ii =>
      sram_in load X(ii :: ii + size)
      Foreach(0 until size) { jj =>
        sram_out(jj) = sram_in(jj)
      }
      Y(ii :: ii + size) store sram_out
    }
  }

  @virtualize
  @api def testreg(
    Z: Reg[Float], // Output List
    Y: Reg[Float], // Output List
    X: Reg[Float]  // Input List
  ): Unit = {
    Pipe {
      Y := X + 2.0.to[Float]
      Z := X + Y
    }
  }
}

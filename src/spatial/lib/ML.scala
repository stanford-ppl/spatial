package spatial.lib

import forge.tags._
import spatial.dsl._
import spatial.metadata.memory._

object ML extends HostML {

  @virtualize 
  /*
   * Tiled dot product
   * @param N vector width
   * @param ip inner loop parallelization
   * @param x lambda that takes in an index and return an element in the first vector
   * @param y lambda that takes in an index and return an element in the second vector
   * */
  @api def dp_flat[T:Num](
    N:scala.Int,
    ip:scala.Int,
  )(input:I32 => (T,T)):T = {
    val dot = Reg[T]
    Reduce(dot)(N par Math.min(ip,N)) { i =>
      val (x,y) = input(i)
      x * y
    } { _ + _ }
    dot.value
  }

  /*
   * Tiled dot product
   * @param N vector width
   * @param ts tiling factor
   * @param op outer loop parallelization
   * @param ip inner loop parallelization
   * @param x lambda that takes in an index and return an element in the first vector
   * @param y lambda that takes in an index and return an element in the second vector
   * */
  @api def dp_tiled[T:Num](
    N:scala.Int,
    ts:scala.Int,
    op:scala.Int,
    ip:scala.Int,
  )(input:I32 => (T,T)):T = {
    def InnerDot(ts:scala.Int, io:Int) = dp_flat(ts, ip) { ii => input(io+ii) }
    N match {
      case N if N <= ts => InnerDot(N, 0)
      case _ => 
        val dot = Reg[T]
        Reduce(dot)(N by ts par Math.min(N/ts,op)) { io =>
          InnerDot(ts, io)
        } { _ + _ }
        dot.value
    }
  }

  /*                                                       
   *                     o
   *     i          +----------+             o
   *  +-----+   x  i|    w     |   =    +----------+
   *  +-----+       |          |        +----------+
   *                +----------+
   * tiled on-chip neural net layer
   * @param ip inner reduce par. If the I dimension == ip, the reduction dimension will not be
   * tiled.
   * @param inPar number of parallel tiles of the inner reduce
   * i: Reduction dimension is tiled by ip
   * @param in Can either be a input SRAM1 or a lambda which takes a index and return a T
   * @param out Can either be a input SRAM1 or a lambda which takes a index and a data T and writes
   * to an externally allocated SRAM
   * @return if output dimension is 1, then return a Some(of the element), otherwise None
   * */
  @api def denselayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
    in:Either[I32 => T, SRAM1[T]],
    out:Either[(I32, T) => Unit, SRAM1[T]],
  ):Option[T] = {
    val dims = w.constDims
    val I = dims(0)
    val O = dims(1)

    def InnerNN(o:Int) = {
      val dot = dp_tiled(I,ip,inPar,ip) { i => 
        val input = in match {
          case Left(read) => read(i)
          case Right(in) => in(i)
        }
        (input, w(i,o))
      }
      val d = activation(dot + b(o))
      out match {
        case Left(write) => write(o,d)
        case Right(out) => out(o) = d
      }
      d
    }
    O match {
      case 1 => Some(InnerNN(0))
      case _ =>
        Foreach(O par outPar) { o =>
          InnerNN(o)
        }
        None
    }
  }
  @api def denselayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
  )(in:I32 => T)(out:(Idx, T) => Unit):Option[T] = 
    denselayer_tiled(w,b,ip,inPar,outPar,activation,Left(in),Left(out))

  @api def denselayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
    in:SRAM1[T],
  )(out:(Idx, T) => Unit):Option[T] = 
    denselayer_tiled(w,b,ip,inPar,outPar,activation,Right(in),Left(out))

  @api def denselayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
    out:SRAM1[T],
  )(in:I32 => T):Option[T] = 
    denselayer_tiled(w,b,ip,inPar,outPar,activation,Left(in),Right(out))

  @api def denselayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
    in:SRAM1[T],
    out:SRAM1[T],
  ):Option[T] = 
    denselayer_tiled(w,b,ip,inPar,outPar,activation,Right(in),Right(out))

  // Activation Functions
  @api def activation[T:Num](x:T => T) = x
  @api def relu[T:Num](x:T) = max(x,0.to[T])

   /*
    * SVM regression inference
    * */
  @api def SVMR_infer[T:Num](V:scala.Int, vp:scala.Int, b:T)(inputs:I32 => (T,T,T)):T = {
    val sum = Reg[T]
    Reduce(sum)(V by 1 par vp) { v =>
      val ins = inputs(v)
      val a = ins._1
      val y = ins._2
      val K = ins._3
      a * y * K
    } { _ + _ }
    sum.value + b
  }

   /*
    * SVM classification inference
    * */
  @api def SVMC_infer[T:Num](V:scala.Int, vp:scala.Int, b:T)(inputs:I32 => (T,T,T)):Bit = {
    SVMR_infer[T](V,vp,b)(inputs) > 0.to[T]
  }

  /*
   * SVM inner product kernel
   * (<x,y> + c)^d
   * */
  @api def inner_kernel[T:Num](N:scala.Int,ip:scala.Int)(vecs:I32 => (T,T)) = {
    dp_flat[T](N, ip)(vecs)
  }

  /*
   * SVM polynomial kernel
   * (<x,y> + c)^d
   * */
  @api def polynomial_kernel[T:Num](c:T,d:T,N:scala.Int,ip:scala.Int)(vecs:I32 => (T,T)) = {
    pow(dp_flat[T](N, ip)(vecs) + c, d)
  }


}

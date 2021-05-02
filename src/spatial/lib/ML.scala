package spatial.lib

import forge.tags._
import spatial.dsl._
import spatial.metadata.memory._
import spatial.lang.types._
import argon._

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
    sum_flat[T](N, ip) { i => val (a,b) = input(i); a * b }
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
    sum_tiled[T](N, ts, op, ip) { i => val (a,b) = input(i); a * b }
  }

  @api def sum_flat[T:Num](
    N:scala.Int,
    ip:scala.Int,
  )(input:I32 => T):T = {
    N match {
      case 1 => 
        input(0.to[I32])
      case _ =>
        val sum = Reg[T]
        Reduce(sum)(N par Math.min(ip,N)) { i => input(i) } { _ + _ }
        sum.value
    }
  }

  @api def sum_tiled[T:Num](
    N:scala.Int,
    ts:scala.Int,
    op:scala.Int,
    ip:scala.Int,
  )(input:I32 => T):T = {
    def inner(ts:scala.Int, io:Int) = sum_flat(ts, ip) { ii => input(io+ii) }
    N match {
      case N if N <= ts => inner(N, 0)
      case _ => 
        val totalSum = Reg[T]
        Reduce(totalSum)(N by ts par Math.min(N/ts,op)) { io =>
          inner(ts, io)
        } { _ + _ }
        totalSum.value
    }
  }
  

  /*                                                       
   * Forward MLP with parameterized layers and dimensions
   * @param weights A list of 2D weights for each layer, SRAM or LUT
   * @param b A list of 1D weights for each layer, SRAM or LUT
   * @param activation activation function
   * @param input input layer access function
   * @param output final output layer update function
   * @param ips a list of inner loop unrolling factor on input dimension of each layer
   * @param mps a list of outer loop unrolling factor on input dimension of each layer
   * @param ops a list of outer loop unrolling factor on output dimension of each layer
   * */
  @api def mlp_forward[T:Num](
    weights:Seq[Sym[_] with TensorMem[T] with ReadMem2[T]], 
    biases:Seq[Sym[_] with TensorMem[T] with ReadMem1[T]], 
    activation: T => T,
    input:I32 => T,
    output:(I32, T) => scala.Unit,
  )(
    ips:Seq[scala.Int]=List.fill(weights.size)(16),
    mps:Seq[scala.Int]=List.fill(weights.size)(1),
    ops:Seq[scala.Int]=List.fill(weights.size)(1),
  ):Unit = {
    val dims = weights.head.constDims.head +: biases.map { _.constDims.head }
    val layers = List.tabulate(dims.size) { i => i }
    val hiddenDims = dims.slice(1,layers.size-1)
    val hiddens = hiddenDims.map { h => SRAM[T](h) }
    layers.sliding(2,1).foreach { case List(prev,next) =>
      val in = IfElse[I32 => T](prev==0) { input } { hiddens(prev-1)(_) }
      val out = IfElse[(I32, T) => scala.Unit](next==layers.last) { output } { hiddens(next-1).update }
      val activation = IfElse(next==layers.last) { identity[T] } { relu[T] }
      val I = dims(prev)
      val O = dims(next)
      val ip = ips(prev)
      val mp = mps(prev)
      val op = ops(prev)
      scala.Predef.assert(ip <= I, s"ip=$ip, I=$I")
      scala.Predef.assert(mp <= I/ip, s"ip=$ip, mp=$mp, I=$I")
      scala.Predef.assert(op <= O, s"op=$op, O=$O")
      val w = weights(prev)
      val b = biases(prev)
      denselayer[T](w, b, activation, in=in, nlout=out)(ip=ip, mp=mp, op=op)
    }
  }

  /*                                                       
   *                     o
   *     i          +----------+             o
   *  +-----+   x  i|    w     |   =    +----------+
   *  +-----+       |          |        +----------+
   *                +----------+
   * Tiled on-chip dense layer
   * @param w weights
   * @param b bias
   * @param activation activation function
   * @param in input layer access function
   * @param lout linear output layer update function
   * @param nlout non-linear output layer update function
   * @return if output dimension is 1, then return a Some(of the element), otherwise None
   * */
  @api def denselayer[T:Num](
  // wrapper, o', off-chip for w and b, on-chip w' and b', list of active IDs, slided load into FIFO into SRAM compressed, capstan feature
    w:Sym[_] with TensorMem[T] with ReadMem2[T], 
    b:Sym[_] with TensorMem[T] with ReadMem1[T], 
    activation: T => T,
    in:I32 => T,
    nlout:(I32, T) => scala.Unit,
    lout:(I32, T) => scala.Unit = { (i:I32,d:T) => () },
  )(
    ip:scala.Int=16,
    mp:scala.Int=1,
    op:scala.Int=1, 
  ):Option[T] = {
    val dims = w.constDims
    val I = dims(0) // 8
    val O = dims(1) // 4

    def InnerNN(o:Int) = {
      val dot = dp_tiled(N=I,ts=ip,op=mp,ip=ip) { i => 
        (in(i), w(i,o))
      }
      val lo = dot + b(o)
      lout(o,lo)
      val nlo = activation(lo)
      nlout(o,nlo)
      nlo
    }
	
    O match {
      case 1 => Some(InnerNN(0))
      case _ =>
        Foreach(O par op) { o =>
          InnerNN(o)
        }
        None
    }
  }
  
  
  @api def sparselayer[T:Num](
  // wrapper, o', off-chip for w and b, on-chip w' and b', list of active IDs, slided load into FIFO into SRAM compressed, capstan feature
    w:Sym[_] with TensorMem[T] with ReadMem2[T], 
    b:Sym[_] with TensorMem[T] with ReadMem1[T], 
    activation: T => T,
    in:I32 => T,
    nlout:(I32, T) => scala.Unit,
    lout:(I32, T) => scala.Unit = { (i:I32,d:T) => () },
  )(
    ip:scala.Int=16,
    mp:scala.Int=1,
    op:scala.Int=1, 
  ):Option[T] = {
    val dims = w.constDims
    val I = dims(0) // 8
    val O = dims(1) // 4

    def InnerNN(o:Int) = {
      val dot = dp_tiled(N=I,ts=ip,op=mp,ip=ip) { i => 
        (in(i), w(i,o))
      }
      val lo = dot + b(o)
      lout(o,lo)
      val nlo = activation(lo)
      nlout(o,nlo)
      nlo
    }
	
    O match {
      case 1 => Some(InnerNN(0))
      case _ =>
        Foreach(O par op) { o =>
          InnerNN(o)
        }
        None
    }
  }
  

  /*                                                       
   *   b                            o
   *  +--+          o           +----------+ 
   * i|in|   x   ---------     i|    w     |
   *  |  |     b |  out  |  =   |          |
   *  +--+       ---------      +----------+
   * Tiled on-chip dense layer backward propagation
   * @param w weights [IxO] 
   * @param b bias [O] 
   * @param batch batch size
   * @param learnRate learning rate
   * @param dactivation derivative of activation function, taking input and output of the activation
   * function as inputs.
   * @param in input layer access function
   * @param nlout non-linear output layer access function
   * @param lout linear output layer access function
   * @param dnlout derivative of non-linear output
   * to an externally allocated SRAM
   * */
  @api def denselayer_backward[T:Num](
    w:SRAM2[T], 
    b:SRAM1[T], 
    batch:scala.Int,
    learnRate:scala.Float,
    dactivation: (T,T) => T, // relu, identity
    in:(I32, I32) => T,
    nlout:(I32, I32) => T, // not used
    lout:(I32, I32) => T, // not used
    dnlout:(I32, I32) => T, // error
  )(
    opb:scala.Int = 1,
    tsb:scala.Int = 16,
    mpb:scala.Int = 1,
    ipb:scala.Int = 16,
    opo:scala.Int = 1,
    tso:scala.Int = 16,
    mpo:scala.Int = 1,
    ipo:scala.Int = 16,
    opi:scala.Int = 1,
  ):SRAM2[T] = {
    val dims = w.constDims
    val I = dims(0)
    val O = dims(1)
    val din = SRAM[T](batch, I)
	
    
    Foreach(0 until batch par opb) { b =>
      Foreach(0 until I par opi) { i =>
        val dot = sum_tiled(O, tso, mpo, ipo) { o =>
          w(i,o) * dactivation(lout(b,o),nlout(b,o)) * dnlout(b,o)
        }
        din(b,i) = dot // error of previous layer = w * error of this layer
      }
    }
    
	Foreach(0 until I par opi) { i =>
      Foreach(0 until O par opo) { o =>
        val wdot = sum_tiled(batch, tsb, mpb, ipb) { b => 
          in(b,i) * dactivation(lout(b,o),nlout(b,o)) * dnlout(b,o)
        }
        w(i, o) = w(i, o) - wdot / batch * learnRate.to[T] // delta w = input of this layer * error of this layer
      }
    }
    
	Foreach(0 until O par opo) { o =>
      val sum = sum_tiled(batch, tsb, mpb, ipb) { b =>
        dactivation(lout(b,o),nlout(b,o)) * dnlout(b,o)
      }
      b(o) = b(o) - sum / batch * learnRate.to[T] // delta b = error of this layer
    }
    
	din
  }

  @api def loss_squre_backward[T:Num](yhat:T, y:T):T = yhat - y

  // Activation Functions
  @api def identity[T:Num]: T => T = { x => x}
  // @api def identity[T:Num](x:T) = x
  @api def identity_backward[T:Num]: (T,T) => T = { (x,y) => 1.to[T]}
  @api def relu[T:Num](x:T) = max(x,0.to[T])
  @api def relu_backward[T:Num](x:T,y:T) = mux(x > 0.to[T], 1.to[T], 0.to[T])

   /*
    * SVM regression inference
    * */
  @api def SVMR_infer[T:Num](V:scala.Int, opv:scala.Int, b:T)(inputs:I32 => (T,T,T)):T = {
    val sum = Reg[T]
    Reduce(sum)(V by 1 par opv) { v =>
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
  @api def SVMC_infer[T:Num](V:scala.Int, opv:scala.Int, b:T)(inputs:I32 => (T,T,T)):Bit = {
    SVMR_infer[T](V,opv,b)(inputs) > 0.to[T]
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

package spatial.lib

import forge.tags._
import scala.collection.mutable.{Seq => MSeq}

trait HostML {

  def unstaged_dp[T:Numeric](x:Seq[T],y:Seq[T]) = {
    val num = implicitly[Numeric[T]]
    import num._
    x.zip(y).map { case (a,b) => a * b }.reduce { _ + _ }
  }

  // Forward: activation(w^T*x+b)
  def unstaged_denselayer[T:Numeric](
    in:Seq[T], // [I]
    w:Seq[Seq[T]], // [I,O]
    b:Seq[T], // [O]
    activation: T => T
  ):(Seq[T], Seq[T]) = {
    val num = implicitly[Numeric[T]]
    import num._
    val wT = w.transpose // [O,I]
    // linear output
    val lout = wT.zip(b).map { case (ws, bb) =>
      val dot = unstaged_dp(ws, in)
      dot + bb
    }
    // nonlinear outout
    val nlout = lout.map { activation }
    (nlout, lout)
  }

  /*
   * @return dW [I,O]
   * @return db [O]
   * dactive = [O]
   * dW = x * dactive^T
   * db = dactive
   * */
  def unstaged_denselayer_backward[T:Fractional](
    W:MSeq[MSeq[T]],
    B:MSeq[T],
    in:Seq[Seq[T]], // [B,I] x
    nlout:Seq[Seq[T]], // [B,O] act(Wx+b)
    lout:Seq[Seq[T]], // [B,O] Wx+b
    dnlout:Seq[Seq[T]], // [B,O]
    dactivation: (T,T) => T,
    learnRate:T,
  ):(Seq[Seq[T]],Seq[T],Seq[Seq[T]]) = {
    val num = implicitly[Fractional[T]]
    import num._
    val batch = num.fromInt(in.size)
    val dlout = (dnlout,nlout,lout).zipped.map { (dnlout,nlout,lout) => 
      (dnlout,nlout,lout).zipped.map { (dnlout,nlout,lout) =>
        dactivation(lout,nlout) * dnlout
      }
    } // [B,O]
    val inT = in.transpose // [I,B]
    val dloutT = dlout.transpose // [O,B]
    val dW = inT.map { x => 
      dloutT.map { dlout =>
        (x,dlout).zipped.map { _ * _ }.sum / batch * learnRate
      }
    } // [I,O]
    val dB = dloutT.map { _.sum / batch * learnRate } // [O]
    val din = dlout.map { dlout => W.map { W => unstaged_dp(W, dlout) } } // [B,I]
    // Update weights
    (0 until W.size).foreach { i =>
      (0 until W(i).size).foreach { j =>
        W(i)(j) -= dW(i)(j)
      }
    }
    (0 until B.size).foreach { i =>
      B(i) -= dB(i)
    }
    (dW, dB, din)
  }

  def unstaged_loss_square[T:Fractional](yhat:T, y:T) = {
    val frac = implicitly[Fractional[T]]
    import frac._
    val half = one / (one * fromInt(2))
    val err = yhat - y
    half * err * err 
  }

  def unstaged_loss_square_backward[T:Fractional](yhat:T, y:T) = {
    val frac = implicitly[Fractional[T]]
    import frac._
    yhat - y
  }
  
  def unstaged_relu[T:Numeric](x:T):T = {
    val num = implicitly[Numeric[T]]
    import num._
    max(x,zero)
  }

  def unstaged_identity[T](x:T):T = {
    x
  }

  def unstaged_identity_backward[T:Numeric](x:T,y:T):T = {
    val num = implicitly[Numeric[T]]
    num.one
  }

  def unstaged_relu_backward[T:Numeric](x:T, y:T):T = {
    val num = implicitly[Numeric[T]]
    import num._
    if (x>num.zero) num.fromInt(1) else num.fromInt(0)
  }

  /*
   * (<x,y> + c)^d
   * */
  def unstaged_inner_kernel[T:Numeric](x:Seq[T],y:Seq[T]) = {
    unstaged_dp(x,y)
  }

  /*
   * (<x,y> + c)^d
   * */
  def unstaged_polynomial_kernel[T:Numeric](c:T,d:Int)(x:Seq[T],y:Seq[T]) = {
    val num = implicitly[Numeric[T]]
    import num._
    val tmp = unstaged_dp(x,y) + c
    (0 until d).map { _ => tmp }.reduce { _ * _ }
  }

  /*
   * SVM regression prediction
   * */
  def unstaged_SVMR_infer[T:Numeric](
    alphas:Seq[T], 
    b:T, 
    supportVectors:Seq[(Seq[T], Boolean)],
    kernel:(Seq[T],Seq[T]) => T
  )(x:Seq[T]):T = {
    val num = implicitly[Numeric[T]]
    import num._
    alphas.zip(supportVectors).map { case (a,(vec,y)) =>
      val yy:T = if (y) one else -one
      a * yy * kernel(x, vec)
    }.sum + b
  }

  /*
   * SVM classification prediction
   * */
  def unstaged_SVMC_infer[T:Numeric](
    alphas:Seq[T], 
    b:T, 
    supportVectors:Seq[(Seq[T], Boolean)],
    kernel:(Seq[T],Seq[T]) => T
  )(x:Seq[T]):Boolean = {
    val num = implicitly[Numeric[T]]
    import num._
    unstaged_SVMR_infer[T](alphas, b, supportVectors, kernel)(x) > zero
  }

  def unstaged_sigmoid(x:Float) = {
    1 / (1 + math.exp(-x).toFloat) 
  }

}

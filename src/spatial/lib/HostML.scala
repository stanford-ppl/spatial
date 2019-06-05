package spatial.lib

import forge.tags._

trait HostML {

  def unstaged_dp[T:Numeric](x:Seq[T],y:Seq[T]) = {
    val num = implicitly[Numeric[T]]
    import num._
    x.zip(y).map { case (a,b) => a * b }.reduce { _ + _ }
  }

  def unstaged_denselayer[T:Numeric](
    in:Seq[T],
    w:Seq[Seq[T]],
    b:Seq[T],
    activation: T => T
  ):Seq[T] = {
    val num = implicitly[Numeric[T]]
    import num._
    val wT = w.transpose
    wT.zip(b).map { case (ws, bb) =>
      val dot = unstaged_dp(ws, in)
      activation(dot + bb)
    }
  }
  
  def unstaged_relu[T:Numeric](x:T) = {
    val num = implicitly[Numeric[T]]
    import num._
    max(x,zero)
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

}

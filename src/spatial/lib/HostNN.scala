package spatial.lib

import forge.tags._

trait HostNN {

  def host_denselayer[T:Numeric](
    in:Seq[T],
    w:Seq[Seq[T]],
    b:Seq[T],
    activation: T => T
  ):Seq[T] = {
    val num = implicitly[Numeric[T]]
    val wT = w.transpose
    wT.zip(b).map { case (ws, bb) =>
      val dot = ws.zip(in).map { case (a,b) => num.times(a, b) }.reduce { num.plus(_,_) }
      activation(num.plus(dot, bb))
    }
  }
  
  def host_relu[T:Numeric](x:T) = {
    val num = implicitly[Numeric[T]]
    num.max(x,num.zero)
  }
}

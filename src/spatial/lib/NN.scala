package spatial.lib

import forge.tags._
import spatial.dsl._
import spatial.metadata.memory._

object NN {

  /*                                                       
   *                     o
   *     i          +----------+             o
   *  +-----+   x  i|    w     |   =    +----------+
   *  +-----+       |          |        +----------+
   *                +----------+
   * tiled on-chip neural net layer
   * i: Reduction dimension is tiled by ip
   * @param in: Can either be a input SRAM1 or a lambda which takes a index and return a T
   * @param out: Can either be a input SRAM1 or a lambda which takes a index and a data T and writes
   * to an externally allocated SRAM
   * @return if output dimension is 1, then return a Some(of the element), otherwise None
   * */
  @virtualize 
  @api def nnlayer_tiled[T:Num](
    w:LUT2[T], 
    b:LUT1[T], 
    ip:scala.Int,
    inPar:scala.Int,
    outPar:scala.Int, 
    activation: T => T,
    in:Either[I32 => T, SRAM1[T]],
    out:Either[(Idx, T) => Unit, SRAM1[T]],
  ):Option[T] = {
    val dims = w.constDims
    val I = dims(0)
    val O = dims(1)

    def InnerNN(o:Int) = {
      val dot = Reg[T]
      Reduce(dot)(I par inPar by ip) { io =>
        val dotInner = Reg[T]
        Reduce(dotInner)(ip par ip) { ii =>
          val i = io + ii
          val input = in match {
            case Left(read) => read(i)
            case Right(in) => in(i)
          }
          input * w(i, o)
        } { _ + _ }
        dotInner.value
      } { _ + _ }
      val d = activation(dot.value + b(o))
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
  implicit def mem_to_either[T:Num](x:SRAM1[T]):Either[I32 => T, SRAM1[T]] = Right(x)
  implicit def mem_to_either2[T:Num](x:SRAM1[T]):Either[(Idx, T) => Unit, SRAM1[T]] = Right(x)

  @virtualize 
  @api def relu[T:Num](x:T) = max(x,0.to[T])

}

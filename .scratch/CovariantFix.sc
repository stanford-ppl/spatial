import scala.annotation.unchecked.{uncheckedVariance => uV}

abstract class Num[A] {
  def +(that: A): A
  def -(that: A): A
  def *(that: A): A
  def /(that: A): A
}

trait _0
trait _32 //extends _0
trait _64 //extends _32
trait TRUE
trait FALSE

class Fmt[F](val s: Boolean, val i: Int, val f: Int)
implicit object IntFmt extends Fmt[(TRUE,_32,_0)](true,32,0)
implicit object LongFmt extends Fmt[(TRUE,_64,_0)](true,64,0)

class Fix[F:Fmt](val v: Double) extends Num[Fix[F]] {
  val fmt: Fmt[F] = implicitly[Fmt[F]]

  def +(that: Fix[F]): Fix[F] = new Fix[F](this.v + that.v)
  def -(that: Fix[F]): Fix[F] = new Fix[F](this.v - that.v)
  def *(that: Fix[F]): Fix[F] = new Fix[F](this.v * that.v)
  def /(that: Fix[F]): Fix[F] = new Fix[F](this.v / that.v)

  override def toString = s"Fix[${fmt.s},${fmt.i},${fmt.f}]($v)"
}

case class FixAdd[F](a: Fix[F], b: Fix[F])
case class FixSub[F](a: Fix[F], b: Fix[F])
case class FixMul[F](a: Fix[F], b: Fix[F])
case class FixDiv[F](a: Fix[F], b: Fix[F])

val x = new Fix[(TRUE,_32,_0)](32)
val y = new Fix[(TRUE,_64,_0)](64)

// Disallowed
//val q = x + y

// Allowed:
val x2 = x + x
val y2 = y + y
type FixPt[S,I,F] = Fix[(S,I,F)]
type I[W] = FixPt[TRUE,W,_0]
type Idx = I[_]

def test(x: Idx): List[Idx] = List(x)
def test2(list: Seq[Idx]): Seq[Idx] = List(x) ++ list


test(x)
test(y)
test2(Seq(x))
test2(Seq(y))

trait Implicits extends LowPriority {
  implicit def ptrMayBeY[A:Y]: Y[Ptr[A]] = new Y[Ptr[A]] { def cs = Seq(y[A]) }
}

trait LowPriority { this: Implicits =>
  implicit def ptrIsX[A:X]: X[Ptr[A]] = new X[Ptr[A]] { def cs = Seq(x[A]) }
}

trait X[A] { def cs: Seq[X[_]] }
trait Y[A] extends X[A] { def test: Int = 4 }

def x[A:X]: X[A] = implicitly[X[A]]
def y[A:Y]: Y[A] = implicitly[Y[A]]

case class Ptr[A](x: A)

object O extends Implicits
import O._

def testY[A:Y](a: A): Unit = { y[A].cs.foreach{println}; println(y[A].test) }
def testX[A:X](a: A): Unit = x[A].cs.foreach{println}

implicit object IntIsX extends X[Int] { def cs = Nil }
implicit object StrIsY extends Y[String] { def cs = Nil }
val a = Ptr(32)
val b = Ptr("hi")

testX(a)

testX(b)
testY(b)


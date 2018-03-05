
class T1
class T2 extends T1
class T3 extends T2

def test[A>:T1](x: A): A = x

val m = new T1
val n = new T2
val o = new T3

test(m)
test(n)
test(o)

def test2(x: AnyVal): Unit = {
  println(x)
}
test2("heillo")

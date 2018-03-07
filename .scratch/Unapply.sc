class MyClass(val x: Int, val y: Int)

object Extract {
  def unapply(x: Any): Option[(Int,Int)] = {
    if (x.isInstanceOf[MyClass]) {
      val myclass = x.asInstanceOf[MyClass]
      Some((myclass.x, myclass.y))
    }
    else None
  }
}

val x = new MyClass(3,2)

x match {
  case inst @ Extract(a,b) => println(s"$inst, $a, $b")
  case _ => println("nerp")
}

class Test1 {
  var x: Int = _
  var y: Int = _

  def this(x1: Int, y1: Int) = { this(); x = x1; y = y1 }

  println("Test1: " + x)
}

class Test2 extends Test1 {
  def this(x1: Int, y1: Int) = super(x1,x2)
  println("Test2: " + x)
}

val t = new Test2(32,16)
println(t.x)
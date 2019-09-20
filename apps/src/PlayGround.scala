import spatial.dsl._


@spatial object IfThenElseString extends SpatialApp {

  def main(args: Array[String]): Unit = {
    val b: Bit = args(0).to[Bit]
    val t1: Text = args(1)
    val t2: Text = args(2)
    val x = if (!b) "a" else "b"
    val y = if (!b) "a" else t1
    val z = if (!b) t1 else "a"
    val q = if (!b) t1 else t2
    println(x + y + z + q)
    assert(x + y + z + q == "baab")
  }

}

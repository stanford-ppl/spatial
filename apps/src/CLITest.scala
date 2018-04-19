import spatial.dsl._

@spatial object CLITest {
  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int] * 32
    val y = args(1)
    val z = args(2).to[Float]
    val q = (args(4) + "32").to[Int] * 55 - 3

    val f = x + q

    println(r"x: $x, y: $y, z: $z, q: $q, f: $f")
  }
}

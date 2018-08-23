package spatial.tests.syntax

import spatial.dsl._


@spatial class IfThenElseBoolean extends SpatialTest {
  override def runtimeArgs: Args = "false false true"

  def main(args: Array[String]): Unit = {
    val b: Bit = args(0).to[Bit]
    val t1: Bit = args(1).to[Bit]
    val t2: Bit = args(2).to[Bit]
    val x: Bit = if (!b) false else true
    val y: Bit = if (!b) t1 else false
    val z: Bit = if (!b) false else t2
    val q: Bit = if (!b) t1 else t2
    println(x & y & z & q)
    assert(x); assert(!y); assert(z); assert(q)
  }
}

@spatial class IfThenElseInt extends SpatialTest {
  override def runtimeArgs: Args = "true 1 2"

  def main(args: Array[String]): Unit = {
    val b: Bit = args(0).to[Bit]
    val t1: Int = args(1).to[Int]
    val t2: Int = args(2).to[Int]
    val x: Int = if (!b) 1 else 0
    val y: Int = if (!b) t1 else 0
    val z: Int = if (!b) 0 else t2
    val q: Int = if (!b) t1 else t2
    println(x + y + z + q)
    assert(x + y + z + q == 4)
  }

}

@spatial class IfThenElseDouble extends SpatialTest {
  override def runtimeArgs: Args = "true 0.1 0.2"

  def main(args: Array[String]): Unit = {
    val b: Bit = args(0).to[Bit]
    val t1: Double = args(1).to[Double]
    val t2: Double = args(2).to[Double]
    val x: Double = if (!b) 1.0 else 0.0
    val y: Double = if (!b) t1 else 0
    val z: Double = if (!b) 0 else t2
    val q: Double = if (!b) t1 else t2
    println(x + y + z + q)
    assert(x + y + z + q == 0.4)
  }
}

@spatial class IfThenElseFloat extends SpatialTest {
  override def runtimeArgs: Args = "true 0.1 0.2"

  def main(args: Array[String]): Unit = {
    val b: Bit = args(0).to[Bit]
    val t1: Float = args(1).to[Float]
    val t2: Float = args(2).to[Float]
    val x: Float = if (!b) 1.0f else 0.0f
    val y: Float = if (!b) t1 else 0
    val z: Float = if (!b) 0 else t2
    val q: Float = if (!b) t1 else t2
    println(x + y + z + q)
    assert(x + y + z + q == 0.4)
  }
}


@spatial class IfThenElseString extends SpatialTest {
  override def runtimeArgs: Args = "true a b"

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



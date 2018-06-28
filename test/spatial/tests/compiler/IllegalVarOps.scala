package spatial.tests.compiler

import spatial.dsl._

@spatial class IllegalVarAssignTest extends SpatialTest {
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
    var testVar: Int = 0
    Accel {
      testVar = 32.to[Int]
    }
    println(testVar)
  }
}

@spatial class IllegalVarReadTest extends SpatialTest {
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
    var xx: Int = 32
    Accel {
      val m = xx + 32
      println(m)
    }
  }
}

@spatial class IllegalVarNewTest extends SpatialTest {
  override def backends = RequireErrors(1)
  def main(args: Array[String]): Unit = {
    Accel {
      var xx: Int = 32
      println(xx + 1)
    }
  }
}
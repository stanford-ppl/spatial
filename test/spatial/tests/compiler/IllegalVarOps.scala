package spatial.tests.compiler

import spatial.dsl._

@test class IllegalVarAssignTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
    var testVar: Int = 0
    Accel {
      testVar = 32.to[Int]
    }
    println(testVar)
  }
}

@test class IllegalVarReadTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
    var xx: Int = 32
    Accel {
      val m = xx + 32
      println(m)
    }
  }
}

@test class IllegalVarNewTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)
  def main(args: Array[String]): Unit = {
    Accel {
      var xx: Int = 32
      println(xx + 1)
    }
  }
}
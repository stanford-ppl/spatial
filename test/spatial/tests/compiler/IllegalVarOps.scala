// package spatial.tests.compiler

// import spatial.dsl._

@test class IllegalVarAssignTest extends SpatialTest { // ReviveMe (write to var)
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
//     var x = 0
//     Accel {
//       x = 32
//     }
//     println(x)
  }
}

@test class IllegalVarReadTest extends SpatialTest { // ReviveMe (write to var)
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)

  def main(args: Array[String]): Unit = {
//     var x = 32
//     Accel {
//       val m = x + 32
//       println(m)
//     }
  }
}

@test class IllegalVarNewTest extends SpatialTest { // ReviveMe (write to var)
  override def runtimeArgs: Args = NoArgs
  override def backends = RequireErrors(1)
  def main(args: Array[String]): Unit = {
//     Accel {
//       var x = 32
//       println(x + 1)
//     }
  }
}
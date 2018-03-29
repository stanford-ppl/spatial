import spatial.dsl._
// import utest._

@spatial object ArgInOut {

  def main(): Void = {
    val a = ArgIn[I32]
    val b = ArgOut[I32]
    Accel {
      b := a + 4
    }
    println("b: " + getArg(b))
  }
}
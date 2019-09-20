import spatial.dsl._

@spatial class cgo_scratchpad extends SpatialTest {

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {
    val a = ArgIn[Int]
    val b = ArgIn[Int]
    setArg(a, 1)
    setArg(b, 1)

    Accel{
      val x = SRAM[Int](5,9).flat
      Foreach(5 by 1 par 2, 9 by 1 par 3){(i,j) =>
        x(i,j) = i+j
      }
      println(r"${x(a.value, b.value)}")
    }
  }
}

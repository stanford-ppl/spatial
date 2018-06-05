package spatial.tests.feature.host

import spatial.dsl._

@test class ReadCSV1D extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _16, _16]
    val tilesize = 16
    val data = loadCSV1D[T](s"$DATA/csv/1d.csv", ",")
    val memsize = ArgIn[Int]
    setArg(memsize, data.length.to[Int])
    val srcmem = DRAM[T](memsize)
    setMem(srcmem, data)
    val out = ArgOut[T]

    Accel {
      val fpgamem = SRAM[T](tilesize)
      out := Reduce(Reg[T](0.to[T]))(memsize.value by tilesize){ r =>
        fpgamem load srcmem(r :: r + tilesize)
        Reduce(Reg[T](0.to[T]))(tilesize by 1) { i =>
          fpgamem(i)
        }{_+_}
      }{_+_}
    }

    val result = getArg(out)
    val golden = data.sum

    printArray(data)
    println("Gold sum is " + golden)
    println("Accel sum is " + result)
    assert(golden == result)
  }
}
package spatial.tests.feature.host

import spatial.dsl._

@spatial class ReadSSV1D extends SpatialTest {

  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _16, _16]
    val tilesize = 16
    val data = loadCSV1D[T](s"$DATA/ssv/1d.ssv", " ")
    val memsize = ArgIn[Int]
    setArg(memsize, data.length.to[Int])
    val srcmem = DRAM[T](memsize)
    setMem(srcmem, data)
    val result = ArgOut[T]

    Accel {
      val fpgamem = SRAM[T](tilesize)
      result := Reduce(Reg[T](0.to[T]))(memsize.value by tilesize) { r =>
        fpgamem load srcmem(r :: r + tilesize)
        Reduce(Reg[T](0.to[T]))(-tilesize until 0 by 1) { i =>
          fpgamem(i+tilesize)
        }{_+_}
      }{_+_}
    }


    val r = getArg(result)

    val gold = data.reduce{_+_}

    printArray(data)
    println("Gold sum is " + gold)
    println("Accel sum is " + r)
    val cksum = gold === r
    println("PASS: " + cksum + " (CSV1D)")
    assert(cksum)
  }
}
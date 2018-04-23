package spatial.tests.feature.host


import spatial.dsl._


@test class ReadCSV1D extends SpatialTest {
  override def runtimeArgs: Args = NoArgs


  def main(args: Array[String]): Unit = {
    type T = FixPt[TRUE, _16, _16]
    val tilesize = 16
    val data = loadCSV1D[T]("/remote/regression/data/1d.csv", ",")
    val memsize = ArgIn[Int]
    setArg(memsize, data.length.to[Int])
    val srcmem = DRAM[T](memsize)
    setMem(srcmem, data)
    val result = ArgOut[T]

    Accel {
      val fpgamem = SRAM[T](tilesize)
      result := Reduce(Reg[T](0.to[T]))(memsize.value by tilesize){ r =>
        fpgamem load srcmem(r :: r + tilesize)
        Reduce(Reg[T](0.to[T]))(tilesize by 1) { i =>
          fpgamem(i)
        }{_+_}
      }{_+_}
    }


    val r = getArg(result)

    val gold = data.reduce {_+_}

    writeCSV1D[T](data, "/remote/regression/data/1d_store.csv", ",")
    printArray(data)
    println("Gold sum is " + gold)
    println("Accel sum is " + r)
    assert(gold == r)
  }
}
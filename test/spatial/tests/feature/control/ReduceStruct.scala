package spatial.tests.feature.control


import spatial.dsl._


@test class ReduceStruct extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  @struct case class MinMax(min: Float, max: Float)


  def main(args: Array[String]): Unit = {

    val dramA = DRAM[Float](64)
    val dramB = DRAM[Float](64)

    Accel {
      val a = SRAM[Float](32)
      val b = SRAM[Float](32)

      val omm = Reg[MinMax]
      Reduce(omm)(64 by 32){ii =>
        a load dramA(ii::ii+32)
        b load dramB(ii::ii+32)

        val mm = Reg[MinMax]
        Reduce(mm)(0 until 32){i =>
          MinMax(min(a(i),b(i)), max(a(i),b(i)))
        }{(a,b) =>
          MinMax( min(a.min,b.min), max(a.max,b.max) )
        }
      }{(a,b) =>
        MinMax( min(a.min,b.min), max(a.max,b.max) )
      }
    }
  }
}

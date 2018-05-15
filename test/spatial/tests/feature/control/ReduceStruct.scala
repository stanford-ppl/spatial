package spatial.tests.feature.control

import spatial.dsl._

@test class ReduceStruct extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  @struct case class MinMax(min: Float, max: Float)

  def main(args: Array[String]): Unit = {
    val dramA = DRAM[Float](64)
    val dramB = DRAM[Float](64)
    val outA = ArgOut[Float]
    val outB = ArgOut[Float]

    val dataA = Array.tabulate(64){i => random[Float] }
    val dataB = Array.tabulate(64){i => random[Float] }
    setMem(dramA, dataA)
    setMem(dramB, dataB)

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

      setArg(outA, omm.value.min)
      setArg(outB, omm.value.min)
    }

    val a = getArg(outA)
    val b = getArg(outB)

    val goldA = dataA.zip(dataB){(a,b) => min(a,b) }.reduce{(a,b) => min(a,b) }
    val goldB = dataA.zip(dataB){(a,b) => max(a,b) }.reduce{(a,b) => max(a,b) }

    assert(a == goldA)
    assert(b == goldB)
  }
}

package spatial.tests.feature.unit

import spatial.dsl._

@test class SimpleMap extends SpatialTest {
  override def runtimeArgs: Args = "5.25"

  type Q32 = FixPt[TRUE,_32,_32]

  def main(args: Array[String]): Unit = {
    val N = 128
    val a = args(0).to[Q32]
    val data = Array.tabulate(N){ i => a * i.to[Q32]}

    val q = ArgIn[Q32](N)
    setArg(q, a)
    val x = DRAM[Q32](N)
    val y = DRAM[Q32](N)
    setMem(x, data)

    Accel {
      val xx = SRAM[Q32](N)
      val yy = SRAM[Q32](N)
      xx load x(0::N par 16)
      Foreach(0 until N by 16){i =>
        yy(i) = xx(i) * q
      }
      y(0::N par 16) store yy
    }

    val result = getMem(y)
    val gold = data.map{e => e * a }
    assert(result == gold)
  }

}

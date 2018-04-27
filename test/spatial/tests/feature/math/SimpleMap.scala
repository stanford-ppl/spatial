package spatial.tests.feature.math

import spatial.dsl._

@test class SimpleMap extends SpatialTest {
  override def runtimeArgs: Args = "5.25"

  type Q32 = FixPt[TRUE,_32,_32]

  def main(args: Array[String]): Unit = {
    val N = 128
    val a = args(0).to[Q32]
    val data = Array.tabulate(N){ i => a * i.to[Q32]}

    val x = DRAM[Q32](N)
    setMem(x, data)

    Accel {
      val xx = SRAM[Q32](N)

    }


  }

}

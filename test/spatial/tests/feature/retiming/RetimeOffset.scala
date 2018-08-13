package spatial.tests.feature.retiming

import spatial.dsl._

@spatial class RetimeOffset extends SpatialTest {
  override def backends = DISABLED // TODO: Rewrite

  def main(args: Array[String]): Void = {
    Accel {
      val sram = SRAM[I32](64)
      val reg = Reg[I32](0)
      Foreach(64 par 2){i =>
        sram(i + reg.value) = i
        reg := (reg.value+1)*5
      }
      Foreach(64 par 16){i => println(sram(i)) }
    }
  }
}

package spatial.tests.feature.unit

import spatial.dsl._

@spatial class FatConstructor extends SpatialTest {
  // Should have more than 255 inputs to construct its Foreach kernel

  override def runtimeArgs: Args = "3"
  def main(args: Array[String]): Unit = {
    val x = ArgIn[Int]
    setArg(x, args(0).to[Int])
    val y = ArgOut[Int]
    Accel {
      val many_regs = List.tabulate(512){i => Reg[Int](i)}
      val many_LUTs = List.tabulate(512){i => LUT[Int](3)(0,1,2)}
      Foreach(x by 1){i => 
        y := many_regs.map(_.value).reduceTree{_+_} + many_LUTs.map(_(i%3)).reduceTree{_+_}
      }
    }

    val result = getArg(y)
    val gold = List.tabulate(512){i => i}.reduce{_+_} + ((args(0).to[Int] - 1 )% 3) * 512
    println(r"Got $result, wanted $gold")
    assert(result == gold)
  } 

}


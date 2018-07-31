package spatial.tests.feature.control


import spatial.dsl._

@spatial class SimplePipelined extends SpatialTest { 
  override def runtimeArgs: Args = "3"



  def main(args: Array[String]): Void = {
    val xIn = args(0).to[Int]
    val tileSize = 16

    val x = ArgIn[Int]
    val out1 = ArgOut[Int]
    val out2 = ArgOut[Int]
    val out3 = ArgOut[Int]
    setArg(x, xIn)

    Accel {
      val bram = SRAM[Int](tileSize)
      Foreach(tileSize by 1){ ii => bram(ii) = ii }
      Foreach(tileSize by 1){ ii => 
        val grab = ii === x.value
        val used_val = x.value + ii
        val ctrval = x.value * ii
        'USECTRVAL1.Foreach(ctrval by 1){i => out2 := i}
        'USECTRVAL2.Foreach(ctrval by 1){i => out2 := i}
        if (grab) 'USEGRAB1.Foreach(3 by 1){i => out1 := bram(ii) + used_val}
        Pipe{out2 := bram(ii)}
        if (grab) 'USEGRAB2.Foreach(3 by 1){i => out3 := bram(ii) + used_val}
      }
    }

    println(r"Got:      $out1 $out2 $out3")
    println(r"Expected: ${3*xIn} 15 ${3*xIn}")

    val chkSum = out1.value == 3*xIn && out2.value == 15 && out3.value == 3*xIn
    assert(chkSum)
    println("PASS: " + chkSum + " (SimplePip)")
  }
}

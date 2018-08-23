package spatial.tests.feature.control


import spatial.dsl._

@spatial class SimplePipelined extends SpatialTest { 
  override def runtimeArgs: Args = "3"



  def main(args: Array[String]): Void = {
    val xIn = args(0).to[Int]
    val tileSize = 16

    val x = ArgIn[Int]
    val switchprop1 = ArgOut[Int]
    val out1 = ArgOut[Int]
    val switchprop2 = ArgOut[Int]
    val ctrprop1 = ArgOut[Int]
    val ctrprop2 = ArgOut[Int]
    setArg(x, xIn)

    Accel {
      val bram = SRAM[Int](tileSize)
      Foreach(tileSize by 1){ ii => bram(ii) = ii }
      Foreach(tileSize by 1){ ii => 
        val grab = ii === x.value
        val used_val = x.value + ii
        val ctrval = x.value * ii
        'USECTRVAL1.Foreach(ctrval by 1){i => if (ii == tileSize-1) ctrprop1 := i}
        'USECTRVAL2.Foreach(ctrval by 1){i => if (ii == tileSize-2) ctrprop2 := i}
        if (grab) 'USEGRAB1.Foreach(3 by 1){i => switchprop1 := bram(ii) + used_val}
        Pipe{out1 := bram(ii)}
        if (grab) 'USEGRAB2.Foreach(3 by 1){i => switchprop2 := bram(ii) + used_val}
      }
    }

    println(r"Got:      SwitchProps - $switchprop1 $switchprop2   Dummy - $out1    CtrProps: $ctrprop1 $ctrprop2")
    println(r"Expected: SwitchProps - ${3*xIn} ${3*xIn}   Dummy - 15    CtrProps: ${xIn*15-1} ${xIn*14-1}")

    val chkSum = switchprop1.value == 3*xIn && out1.value == 15 && switchprop2.value == 3*xIn && ctrprop1.value == (xIn*15-1) && ctrprop2.value == (xIn*14-1)
    assert(chkSum)
    println("PASS: " + chkSum + " (SimplePip)")
  }
}

package spatial.tests.apps

import spatial.dsl._
@spatial class TestPopcnt extends SpatialTest { // Test Args: 


  def main(args: Array[String]): Void = {

    val num : UInt32 = 255.to[UInt32]

    val nr = ArgIn[UInt32]
    val popcnt = ArgOut[UInt8]
    val popcnt2 = ArgOut[UInt8]
    
    setArg(nr, num.to[UInt32])
    val seqNum = Seq[Bit](1.to[Bit],1.to[Bit],1.to[Bit],0.to[Bit])
    Accel {
       popcnt := popcount(seqNum)
       popcnt2 := popcount(Seq.tabulate(32){ i => nr.value.bit(i).to[Bit]})       	  
    }

    println(r"Popcount 1 value is ${getArg(popcnt)}")
    println(r"Popcount 2 value is ${getArg(popcnt2)}")
}
}

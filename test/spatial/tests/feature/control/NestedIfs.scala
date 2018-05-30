package spatial.tests.feature.control


import spatial.dsl._


@test class NestedIfs extends SpatialTest {
  override def runtimeArgs: Args = "43" and "2"


  def nestedIfTest(x: Int): Int = {
    val in = ArgIn[Int]
    val out = ArgOut[Int]
    setArg(in, x)
    Accel {
      val sram = SRAM[Int](3)
      if (in >= 42.to[Int]) {     // if (43 >= 42)
        if (in <= 43.to[Int]) {   // if (43 <= 43)
          sram(in - 41.to[Int]) = 10.to[Int] // sram(2) = 10
        }
      }
      else {
        if (in <= 2.to[Int]){
          sram(2) = 20.to[Int]
        }
      }
      Pipe{out := sram(2)}
    }
    getArg(out)
  }


  def main(args: Array[String]): Unit = {
    val x = args(0).to[Int]
    val result = nestedIfTest(x)
    println("result:   " + result)
    if (x == 43) assert(result == 10.to[Int])
    else if (x <= 2) assert(result == 20.to[Int])
  }
}

// @test class NestedIfs1H extends SpatialTest {
//   override def runtimeArgs: Args = NoArgs

//   def nestedIfTest(x: Int): Int = {
//     val in = ArgIn[Int]
//     val out = ArgOut[Int]
//     setArg(in, x)
//     Accel {
//       val ans = if (in >= 42.to[Int]) {     // if (43 >= 42)
//         if (in <= 43.to[Int]) {   // if (43 <= 43)
//           10.to[Int] // sram(2) = 10
//         } 
//         else 5.to[Int]
//       }
//       else {
//         if (in <= 2.to[Int]){
//           20.to[Int]
//         }
//         else 99.to[Int]
//       }
//       out := ans
//     }
//     getArg(out)
//   }


//   def main(args: Array[String]): Unit = {
//     val result = nestedIfTest(43)
//     println("result:   " + result)
//     assert(result == 10.to[Int])
//   }
// }

// @test class NestedIfs1HCtrl extends SpatialTest {
//   override def runtimeArgs: Args = NoArgs


//   def nestedIfTest(x: Int): Int = {
//     val in = ArgIn[Int]
//     val out = ArgOut[Int]
//     setArg(in, x)
//     Accel {
//       val s = SRAM[Int](4)
//       out := if (in >= 42.to[Int]) {     // if (43 >= 42)
//         if (in <= 43.to[Int]) {   // if (43 <= 43)
//           Foreach(4 by 1){i => s(i) = i*10}
//           s(1)
//         } 
//         else 5.to[Int]
//       }
//       else {
//         if (in <= 2.to[Int]){
//           20.to[Int]
//         }
//         else 99.to[Int]
//       }
//     }
//     getArg(out)
//   }


//   def main(args: Array[String]): Unit = {
//     val result = nestedIfTest(43)
//     println("result:   " + result)
//     assert(result == 10.to[Int])
//   }
// }

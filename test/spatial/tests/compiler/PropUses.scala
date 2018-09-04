package spatial.tests.compiler

import argon.Block
import spatial.dsl._

@spatial class PropUses extends SpatialTest {
  override def runtimeArgs: Args = "16"
  def main(args: Array[String]): Unit = {
    @struct case class mystruct(x : I8, y : I8, z : I8)    
    val x = ArgIn[I8]
    val n = args(0).to[I8]
    setArg(x,n)
    val flag = ArgIn[Int]
    val in_triangle = ArgIn[Int]
    setArg(flag, 1)
    setArg(in_triangle, 1)
    val y = ArgOut[Int]
    val z = ArgOut[Int]
    Accel {
      val numiter = x.value.to[I32] // RegRead -> FixToFix -> CounterNew -> Controller used to crash TransientCleanup
      y := Reduce(Reg[Int])(numiter by 1){i => i}{_+_}


      // Ripped from Rendering3D to test issue #114
      Pipe{
        val frag_reg = Reg[Int](0)
        val other_reg = Reg[mystruct](mystruct(0,0,0))
        val fragments = FIFO[mystruct](16)
        other_reg := mystruct(x.value, x.value, x.value)
        val tmp = other_reg.value
        if ( (flag == 1) ) { 
          Foreach(8 by 1, 8 by 1){ (x_t, y_t) =>
            val frag_x = 5.to[I8]
            val frag_y = 7.to[I8]
            val frag_z = tmp.z

            if (in_triangle == 1) {
              //Parallel { 
                fragments.enq( mystruct(frag_x, frag_y, frag_z) )
                frag_reg := frag_reg +  1.to[Int] 
            }
          }
        } 

        z := fragments.deq().z.to[Int] + frag_reg
      }
    }
    val gold = Array.tabulate(args(0).to[Int]){i => i}.reduce{_+_}
    println(r"Want $gold, got ${getArg(y)}")
    println(r"Print z for fun: $z")
    assert(getArg(y) == gold)
  }

}

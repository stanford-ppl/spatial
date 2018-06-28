package spatial.tests.feature.control

import spatial.dsl._

@spatial class FIFOBranch extends SpatialTest { // Regression (Unit) // Args: 13 25
  override def runtimeArgs: Args = "13 25"

  def main(args: Array[String]): Void = {
    val num_enq_1 = ArgIn[Int]
    val num_enq_2 = ArgIn[Int]
    val out = ArgOut[Int]

    setArg(num_enq_1, args(0).to[Int])
    setArg(num_enq_2, args(1).to[Int])

    Accel{
      val fifo1 = FIFO[Int](128)
      val fifo2 = FIFO[Int](128)
      Foreach(num_enq_1 by 1) {i => fifo1.enq(i)}
      Foreach(num_enq_2 by 1) {i => fifo2.enq(i)}
      out := Reduce(Reg[Int])(num_enq_1 + num_enq_2 by 1) {i =>
        if (fifo1.isEmpty) {
          fifo2.deq()
        } else {
          fifo1.deq()
        }
      }{_+_}
    }

    val result = getArg(out)
    val gold = Array.tabulate(args(0).to[Int]){i => i}.reduce{_+_} + Array.tabulate(args(1).to[Int])(i => i).reduce{_+_}
    val cksum = gold == result
    println("Got " + result + ", wanted " + gold)
    println("PASS: " + cksum + " (FIFOBranch)")
    assert(cksum)

  }
}

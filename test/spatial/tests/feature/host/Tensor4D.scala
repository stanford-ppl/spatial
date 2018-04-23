package spatial.tests.feature.host


import spatial.dsl._


@test class Tensor4D extends SpatialTest {
  override def runtimeArgs: Args = "32 4 4 4"


  def main(args: Array[String]): Unit = {
    val tsP = 2
    val tsR = 2
    val tsC = 16
    val tsX = 2
    val P = ArgIn[Int]
    val R = ArgIn[Int]
    val C = ArgIn[Int]
    val X = ArgIn[Int]
    val c = args(0).to[Int]
    val r = args(1).to[Int]
    val p = args(2).to[Int]
    val x = args(3).to[Int]
    setArg(P, p)
    setArg(R, r)
    setArg(C, c)
    setArg(X, x)

    val srcDRAM4 = DRAM[Int](X,P,R,C)
    val dstDRAM4 = DRAM[Int](X,P,R,C)
    val data4 = (0::x, 0::p, 0::r, 0::c){(x,p,r,c) => x+r+c+p /*random[Int](5)*/}
    setMem(srcDRAM4, data4)

    Accel {
      val sram4 = SRAM[Int](tsX,tsP,tsR,tsC)
      Foreach(X by tsX, P by tsP, R by tsR, C by tsC) { case List(h,i,j,k) =>
        sram4 load srcDRAM4(h::h+tsX, i::i+tsP, j::j+tsR, k::k+tsC)
        dstDRAM4(h::h+tsX, i::i+tsP, j::j+tsR, k::k+tsC) store sram4
      }
    }

    // Extract results from accelerator
    val result4 = getTensor4(dstDRAM4)
    printTensor4(result4, "result: ")
    printTensor4(data4, "gold: ")
    assert(result4 == data4)
  }
}

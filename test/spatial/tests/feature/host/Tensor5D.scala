package spatial.tests.feature.host


import spatial.dsl._


@test class Tensor5D extends SpatialTest {
  override def runtimeArgs: Args = "32 4 4 4 4"

  def main(args: Array[String]): Unit = {
    // For 3D
    val tsP = 2
    val tsR = 2
    val tsC = 16
    val tsX = 2
    val tsY = 2
    val P = ArgIn[Int]
    val R = ArgIn[Int]
    val C = ArgIn[Int]
    val X = ArgIn[Int]
    val Y = ArgIn[Int]
    val c = args(0).to[Int]
    val r = args(1).to[Int]
    val p = args(2).to[Int]
    val x = args(3).to[Int]
    val y = args(4).to[Int]
    setArg(P, p)
    setArg(R, r)
    setArg(C, c)
    setArg(X, x)
    setArg(Y, y)

    val srcDRAM5 = DRAM[Int](Y,X,P,R,C)
    val dstDRAM5 = DRAM[Int](Y,X,P,R,C)
    val data5 = (0::y, 0::x, 0::p, 0::r, 0::c){(y,x,p,r,c) => y+x+r+c+p /*random[Int](5)*/}
    setMem(srcDRAM5, data5)

    Accel {
      val sram5 = SRAM[Int](tsY,tsX,tsP,tsR,tsC)
      Foreach(Y by tsY, X by tsX, P by tsP, R by tsR, C by tsC) { case List(g,h,i,j,k) =>
        sram5 load srcDRAM5(g::g+tsY, h::h+tsX, i::i+tsP, j::j+tsR, k::k+tsC)
        dstDRAM5(g::g+tsY, h::h+tsX, i::i+tsP, j::j+tsR, k::k+tsC) store sram5
      }
    }


    // Extract results from accelerator
    val result5 = getTensor5(dstDRAM5)
    printTensor5(result5, "result")
    printTensor5(data5, "gold")
    assert(result5 == data5)
  }
}

package spatial.tests.feature.host


import spatial.dsl._


@spatial class Tensor3D extends SpatialTest {
  override def dseModelArgs: Args = "32 4 4"
  override def finalModelArgs: Args = "32 4 4"
  override def runtimeArgs: Args = "32 4 4"


  def main(args: Array[String]): Unit = {
    // For 3D
    val tsP = 2
    val tsR = 2
    val tsC = 16
    val P = ArgIn[Int]
    val R = ArgIn[Int]
    val C = ArgIn[Int]
    val c = args(0).to[Int]
    val r = args(1).to[Int]
    val p = args(2).to[Int]
    setArg(P, p)
    setArg(R, r)
    setArg(C, c)
    val srcDRAM3 = DRAM[Int](P,R,C)
    val dstDRAM3 = DRAM[Int](P,R,C)
    val data3 = (0::p, 0::r, 0::c){(p,r,c) => r+c+p /*random[Int](5)*/}
    setMem(srcDRAM3, data3)

    Accel {
      val sram3 = SRAM[Int](tsP, tsR, tsC)
      Foreach(P by tsP, R by tsR, C by tsC) { (i,j,k) =>
        sram3 load srcDRAM3(i::i+tsP, j::j+tsR, k::k+tsC)
        dstDRAM3(i::i+tsP, j::j+tsR, k::k+tsC) store sram3
      }
    }

    // Extract results from accelerator
    val result3 = getTensor3(dstDRAM3)
    printTensor3(result3, "got: ")
    printTensor3(data3, "wanted; ")
    println("")
    assert(result3 == data3)
  }
}

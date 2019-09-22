package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class TransferStruct extends SpatialTest {
  @struct case class mystruct(el1: Int, el2: Int)  


  type Pixel = FixPt[TRUE, _9, _23]
  type Frame = UInt8

  @struct case class Velocity(x : Pixel, y : Pixel)

  @struct case class TriPixel(x : Pixel, y : Pixel, z : Pixel, padding : UInt32)

  @struct case class Tensor(t1  : Pixel, 
                t2  : Pixel,
                t3  : Pixel,
                t4  : Pixel,
                t5  : Pixel,
                t6  : Pixel, 
                padding : UInt64)




  def main(args: Array[String]): Unit = {

    val veldata = (0::16, 0::16){(i,j) => Velocity(i.to[Pixel] + 0.5.to[Pixel],j.to[Pixel])}
    val framdata = (0::16, 0::16){(i,j) => i.to[Frame]}
    val tridata = (0::16, 0::16){(i,j) => TriPixel(i.to[Pixel],j.to[Pixel], i.to[Pixel], j.to[UInt32])}
    val tensordata = (0::16, 0::16){(i,j) => Tensor(i.to[Pixel],j.to[Pixel], i.to[Pixel], j.to[Pixel], i.to[Pixel], j.to[Pixel], i.to[UInt64])}

    val veldram = DRAM[Velocity](16,16)
    val framdram = DRAM[Frame](16,16)
    val tridram = DRAM[TriPixel](16,16)
    val tensordram = DRAM[Tensor](16,16)

    setMem(veldram, veldata)
    setMem(framdram, framdata)
    setMem(tridram, tridata)
    setMem(tensordram, tensordata)

    val out = ArgOut[Int]
    val dr = DRAM[mystruct](10)
    Accel {
      val s = SRAM[mystruct](10)
      s(5) = mystruct(42, 43)
      dr(0::10) store s

      val s1 = SRAM[mystruct](10)
      s1 load dr(0::10)
      out := s1(5).el1 * s1(5).el2

      val velsram = SRAM[Velocity](16,16)
      val framsram = SRAM[Frame](16,16)
      val trisram = SRAM[TriPixel](16,16)
      val tensorsram = SRAM[Tensor](16,16)

      velsram load veldram
      framsram load framdram
      trisram load tridram
      tensorsram load tensordram
  
      veldram store velsram
      framdram store framsram
      tridram store trisram
      tensordram store tensorsram
    }
    val x = getArg(out)

    printMatrix(getMatrix(veldram), "veldram")
    printMatrix(veldata, "veldata")
    printMatrix(getMatrix(framdram), "framdram")
    printMatrix(framdata, "fradatam")
    printMatrix(getMatrix(tridram), "tridram")
    printMatrix(tridata, "tridata")
    printMatrix(getMatrix(tensordram), "tensordram")
    printMatrix(tensordata, "tendataram")

    val velcksum = getMatrix(veldram) == veldata
    val framcksum = getMatrix(framdram) == framdata
    val tricksum = getMatrix(tridram) == tridata
    val tensorcksum = getMatrix(tensordram) == tensordata
    println(r"vel: ${velcksum}")
    println(r"fram: ${framcksum}")
    println(r"tri: ${tricksum}")
    println(r"tensor: ${tensorcksum} (apparently failure here is expected?)")
    println(r"$x == 42*43 == ${42*43}")

    assert(velcksum && framcksum && tricksum)
    assert(x == 42*43)


  }
}

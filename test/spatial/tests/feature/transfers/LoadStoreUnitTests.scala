package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class FrameLoadTest extends SpatialTest {
  lazy val N = 16.to[Int]

  def main(args: Array[String]): Unit = {

    val in = FrameIn[U64](8)
    val data = Array[U64](Seq.tabulate(8){i => List.tabulate(4){j => scala.math.pow(2,j).toInt * (i*4+j)}.sum}.map(_.to[U64]):_*)

    setFrame(in,data)
    val out = List.tabulate(32){_ => ArgOut[U16]}
    Accel {
      val a = SRAM[U64](32)
      a load in
      out.grouped(4).zipWithIndex.foreach { case (os, i) =>
        val all = a(i).asVec[U16]
        os.zipWithIndex.foreach { case (o, j) => o := all(j) }
      }
    }
    out.zipWithIndex.foreach{case (o,i) =>
      println(r"got ${getArg(o)}, wanted ${i-1}")
      assert(getArg(o) == i-1)
    }
  }
}
//
//
//@spatial class FrameStoreTest extends SpatialTest {
//  lazy val N = 32.to[Int]
//
//  def main(args: Array[String]): Unit = {
//
//    val out = DRAM[Int](32)
//
//    Accel {
//      val a = SRAM[Int](32)
//      Foreach(32 by 1){i => a(i) = i}
//      out store a
//    }
//    val gold = Array.tabulate(32){i => i}
//    printArray(getMem(out), "Got")
//    assert(gold == getMem(out))
//  }
//}

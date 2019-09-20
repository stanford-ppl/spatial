package spatial.tests.experiment
import spatial.dsl._


//class cgo_denoise0 extends cgo_denoise(0)
//class cgo_denoise1 extends cgo_denoise(1)
//class cgo_denoise2 extends cgo_denoise(2)
//class cgo_denoise3 extends cgo_denoise(3)
class cgo_denoise4 extends cgo_denoise(4)
@spatial abstract class cgo_denoise(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I32]


    Accel {

      val mem =
        if (region == 0) SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I32](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I32](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I32](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
//      Foreach(8 by 1, 8 by 1) { (i, j) => mem(i, j) = i + j }
      val reg = Reg[I32]
      Foreach(6 by 1, 6 by 1) { (i, j) =>  reg := mem(i + 1, j)  | mem(i, j + 1)   | mem(i + 1, j + 2)  | mem(i + 2, j + 1) }
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

//class cgo_deconv0 extends cgo_deconv(0)
//class cgo_deconv1 extends cgo_deconv(1)
class cgo_deconv2 extends cgo_deconv(2)
//class cgo_deconv3 extends cgo_deconv(3)
//class cgo_deconv4 extends cgo_deconv(4)
@spatial abstract class cgo_deconv(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I32]

    Accel{
      val mem =
        if (region == 0) SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I32](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I32](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I32](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = i+j}
      val reg = Reg[I32]
      Foreach(6 by 1, 6 by 1){(i,j) => reg :=  mem(i+1,j+1)  | mem(i+1,j)  | mem(i,j+1)   | mem(i+1,j+2)  | mem(i+2,j+1)}
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

//class cgo_denoise_ur0 extends cgo_denoise_ur(0)
//class cgo_denoise_ur1 extends cgo_denoise_ur(1)
class cgo_denoise_ur2 extends cgo_denoise_ur(2)
//class cgo_denoise_ur3 extends cgo_denoise_ur(3)
//class cgo_denoise_ur4 extends cgo_denoise_ur(4)
@spatial abstract class cgo_denoise_ur(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I32]

    Accel{
      val mem =
        if (region == 0) SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I32](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I32](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I32](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = i+j}
      val reg = Reg[I32]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := mem(i+1,j+1)  | mem(i+1,j)  | mem(i,j+1)   | mem(i+1,j+2)  | mem(i+1,j+3)  | mem(i,j+2)  | mem(i+2,j+2)  | mem(i+2,j+1) }
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

//class cgo_bicubic0 extends cgo_bicubic(0)
//class cgo_bicubic1 extends cgo_bicubic(1)
//class cgo_bicubic2 extends cgo_bicubic(2)
//class cgo_bicubic3 extends cgo_bicubic(3)
class cgo_bicubic4 extends cgo_bicubic(4)
@spatial abstract class cgo_bicubic(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I32]

    Accel{
      val mem =
        if (region == 0) SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I32](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I32](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I32](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = i+j}
      val reg = Reg[I32]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := mem(i,j)  | mem(i,j+2)   | mem(i+2,j+2)  | mem(i+2,j) }
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

class cgo_sobel0 extends cgo_sobel(0)
//class cgo_sobel1 extends cgo_sobel(1)
class cgo_sobel2 extends cgo_sobel(2)
//class cgo_sobel3 extends cgo_sobel(3)
//class cgo_sobel4 extends cgo_sobel(4)
@spatial abstract class cgo_sobel(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I32]

    Accel{
      val mem =
        if (region == 0) SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I32](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I32](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I32](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I32](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = i+j}
      val reg = Reg[I32]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := List.tabulate(3,3){(a,b) => mem(i+a,j+b)}.flatten.reduceTree{_|_}}
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

class cgo_motion_lv0 extends cgo_motion_lv(0)
//class cgo_motion_lv1 extends cgo_motion_lv(1)
//class cgo_motion_lv2 extends cgo_motion_lv(2)
//class cgo_motion_lv3 extends cgo_motion_lv(3)
//class cgo_motion_lv4 extends cgo_motion_lv(4)
@spatial abstract class cgo_motion_lv(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I8]

    Accel{
      val mem =
        if (region == 0) SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I8](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I8](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I8](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = (i+j).to[I8]}
      val reg = Reg[I8]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := List.tabulate(6){a => mem(i+a,j)}.reduceTree{_|_}}
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

class cgo_motion_lh0 extends cgo_motion_lh(0)
//class cgo_motion_lh1 extends cgo_motion_lh(1)
//class cgo_motion_lh2 extends cgo_motion_lh(2)
//class cgo_motion_lh3 extends cgo_motion_lh(3)
//class cgo_motion_lh4 extends cgo_motion_lh(4)
@spatial abstract class cgo_motion_lh(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I8]

    Accel{
      val mem =
        if (region == 0) SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I8](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I8](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I8](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = (i+j).to[I8]}
      val reg = Reg[I8]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := List.tabulate(6){a => mem(i,j+a)}.reduceTree{_|_}}
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}

class cgo_motion_c0 extends cgo_motion_c(0)
//class cgo_motion_c1 extends cgo_motion_c(1)
//class cgo_motion_c2 extends cgo_motion_c(2)
//class cgo_motion_c3 extends cgo_motion_c(3)
//class cgo_motion_c4 extends cgo_motion_c(4)
@spatial abstract class cgo_motion_c(region: scala.Int) extends SpatialTest {

  def main(args: Array[String]): Unit = {
    val probe = ArgOut[I8]

    Accel{
      val mem =
        if (region == 0) SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 1) SRAM[I8](8,8).effort(0).hierarchical.nPow2.alphaPow2.noblockcyclic.nofission
        else if (region == 2) SRAM[I8](8,8).effort(0).flat.nBest.alphaBest.noblockcyclic.nofission
        else if (region == 3) SRAM[I8](8,8).effort(0).flat.nPow2.alphaPow2.noblockcyclic.nofission
        else SRAM[I8](8,8).effort(0).hierarchical.nBest.alphaBest.onlyblockcyclic.nofission
      Foreach(8 by 1, 8 by 1){(i,j) => mem(i,j) = (i+j).to[I8]}
      val reg = Reg[I8]
      Foreach(6 by 1, 6 by 1){(i,j) => reg := List.tabulate(2,2){(a,b) => mem(i+a,j+b)}.flatten.reduceTree{_|_}}
      probe := reg.value
    }
    println(probe)
  assert(random[Int](10) == 3)
  }
}


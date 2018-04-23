package spatial.tests.feature.streaming

import spatial.dsl._

import spatial.targets.DE1

object ShiftReg extends SpatialApp {
  override val target = DE1

  val Rmax = 240
  val Cmax = 320

  type Int16 = FixPt[TRUE,_16,_0]
  type UInt8 = FixPt[FALSE,_8,_0]
  type UInt5 = FixPt[FALSE,_5,_0]
  type UInt6 = FixPt[FALSE,_6,_0]

  @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)


  def convolveVideoStream(rows: Int, cols: Int): Unit = {

    val imgIn  = StreamIn[Pixel16](target.VideoCamera)
    val imgOut = BufferedOut[Pixel16](target.VGA)

    Accel(*) {

      val fifoIn = FIFO[Int16](2*Cmax)
      val sr = RegFile[Int16](1, Cmax/4)

      Foreach(0 until Rmax) { r =>
        Foreach(0 until Cmax) { _ =>
          val pixel = imgIn.value()
          val grayPixel = (pixel.b.to[Int16] + pixel.g.to[Int16] + pixel.r.to[Int16]) / 3
          fifoIn.enq( grayPixel )
        }


        Foreach(0 until Cmax by Cmax/4) { c =>
          Foreach(0 until Cmax/4) { cc =>
            sr(0, *) <<= fifoIn.deq
          }
          Foreach(0 until Cmax/4) { cc =>
            val result = sr(0,cc)
            imgOut(r,c+cc) = Pixel16(result(5::1).as[UInt5], result(5::0).as[UInt6], result(5::1).as[UInt5]) // Technically should be sqrt(horz**2 + vert**2)
          }
        }
      }
      ()
    }
  }


  def main(args: Array[String]): Unit = {
    val R = Rmax
    val C = Cmax
    convolveVideoStream(R, C)
  }
}



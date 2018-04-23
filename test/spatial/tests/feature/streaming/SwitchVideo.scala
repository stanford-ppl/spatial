package spatial.tests.feature.streaming

import spatial.dsl._

import spatial.targets.DE1

object SwitchVideo extends SpatialApp { // BUSTED.  HOW TO USE SWITCHES?
  override val target = DE1

  val Kh = 3
  val Kw = 3
  val Rmax = 240
  val Cmax = 320

  //  @struct case class Pixel24(b: UInt8, g: UInt8, r: UInt8)
  @struct case class Pixel16(b: UInt5, g: UInt6, r: UInt5)
  @struct class sw3(sel: UInt5, unused: UInt5)


  def convolveVideoStream(rows: Int, cols: Int): Unit = {
    val imgOut  = BufferedOut[Pixel16](target.VGA)
    val imgIn   = StreamIn[Pixel16](target.VideoCamera)
    val swInput = StreamIn[sw3](target.SliderSwitch)

    Accel(*) {
      val fifoIn = FIFO[Pixel16](Cmax*2)
      Foreach(0 until Rmax) { i =>
        Foreach(0 until Cmax) { j =>
          val swBits = swInput.value()
          val f0 = swBits.sel
          val pixel = imgIn.value()
          val rpixel = Pixel16(0.to[UInt5], 0.to[UInt6], pixel.r)
          val gpixel = Pixel16(0.to[UInt5], pixel.g, 0.to[UInt5])
          val bpixel = Pixel16(pixel.b, 0.to[UInt6], 0.to[UInt5])
          val funpixel = Pixel16(pixel.r, pixel.b.as[UInt6], pixel.g.as[UInt5])
          val grayVal = (pixel.b.to[Int16] + pixel.g.to[Int16] + pixel.r.to[Int16]) / 3
          val grayPixel = Pixel16(grayVal(5::1).as[UInt5], grayVal(5::0).as[UInt6], grayVal(5::1).as[UInt5])
          val muxpix = mux(f0 <= 0, pixel,
            mux(f0 <= 1, rpixel,
              mux(f0 <= 2, gpixel,
                mux(f0 <= 4, bpixel,
                  mux(f0 <= 8, funpixel,
                    mux(f0 <= 16, grayPixel, pixel))))))
          fifoIn.enq(muxpix)
        }
        Foreach(0 until Cmax) { j =>
          val pixel = fifoIn.deq()
          imgOut(i,j) = Pixel16(pixel.b, pixel.g, pixel.r)
        }
      }
    }
  }


  def main(args: Array[String]): Unit = {
    val R = Rmax
    val C = Cmax
    convolveVideoStream(R, C)
  }
}


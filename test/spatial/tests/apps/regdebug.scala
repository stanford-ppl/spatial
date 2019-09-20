package spatial.tests.apps

import spatial.dsl._
import utils.math._

@spatial class regdebug extends SpatialTest {
  override def runtimeArgs: Args = ""
  
  type T = FixPt[TRUE,_10,_22]

  def main(args: Array[String]): Unit = {

    val debug:scala.Boolean = false
    
    val i0 = loadCSV1D[T]("/home/shadjis/spatial-lang/d.csv", "\n")
    val i0_DRAM = DRAM[T](440)
    val out_DRAM = DRAM[T](2048)
    setMem(i0_DRAM, i0)

    val b = loadCSV1D[T]("/home/shadjis/spatial-lang/b.csv", "\n")
    val b_DRAM = DRAM[T](13994)
    setMem(b_DRAM, b)

    val w = loadCSV1D[T]("/home/shadjis/spatial-lang/w.csv", "\n")
    val w_DRAM = DRAM[T](25366528)
    setMem(w_DRAM, w)

    val debug_out = ArgOut[Int]
    Accel {

      val w_rows    = LUT[Int](1)( 2048)
      val w_cols    = LUT[Int](1)( 440 )

      Pipe{debug_out := 10000.to[Int]}

      val in_SRAM = SRAM[T](2048).nonbuffer
      val out_SRAM = SRAM[T](2048).nonbuffer
      in_SRAM load i0_DRAM(0::440)

      Pipe{debug_out := 20000.to[Int]}

      Sequential.Foreach(0 until 1) { L =>
        Pipe{debug_out := 20000.to[Int] + L.to[Int] + 1.to[Int]}
        val b_SRAM = SRAM[T](2048)
        b_SRAM load b_DRAM(0 :: w_rows(L))
        Foreach(w_rows(L) by 1 par 4) { r =>
          val w_SRAM = SRAM[T](2048)
          w_SRAM load w_DRAM(r.to[Int]*w_cols(L) :: (r.to[Int]+1)*w_cols(L) par 16)
          // Foreach(1 by 1) { br =>
            val tmp1 = Reduce(Reg[T](0.to[T]))(w_cols(L) by 1 par 16){ c => 
              val data = 
                mux(L % 2 == 0, 
                  in_SRAM(c), 
                  out_SRAM(c)
                )
              data * w_SRAM(c)
            }{_+_}
            val tmp2 = tmp1.value + b_SRAM(r)
            val out = mux( L == 0, tmp2, tmp2 )
            if (L % 2 == 0) {
              out_SRAM(r) = out
            } else {
              in_SRAM(r) = out
            }
          // }
        }
      }

      Pipe{debug_out := 30000.to[Int]}

      out_DRAM(0::2048) store out_SRAM
    }

    println(r"${getArg(debug_out)}")
    val output = getMem(out_DRAM)
    printArray(output, "output")
  }
}


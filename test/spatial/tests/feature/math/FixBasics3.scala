package spatial.tests.feature.math


import spatial.dsl._


@test class FixBasics3 extends SpatialTest {
  override def runtimeArgs: Args = "5.25 2.125"

  type T = FixPt[TRUE,_32,_32]


  def main(args: Array[String]): Unit = {
    // Declare SW-HW interface vals
    val N = 128
    val a = args(0).to[T]
    val b = args(1).to[T]
    val TWOPI = 6.28318530717959.toUnchecked[T]
    val x_data = Array.tabulate(N){ i => a * i.to[T]}
    val x = DRAM[T](N)
    val y = DRAM[T](N)
    val s = ArgIn[T]

    val expo_dram = DRAM[T](1024)
    val sin_dram = DRAM[T](1024)
    val cos_dram = DRAM[T](1024)
    val sqroot_dram = DRAM[T](512)

    setMem(x, x_data)
    setArg(s, b)

    Accel {
      val xx = SRAM[T](N)
      val yy = SRAM[T](N)
      xx load x(0 :: N par 16)
      Foreach(N by 1) { i =>
        yy(i) = xx(i) * s
      }
      // Test exp_taylor from -4 to 4
      // NOTE: This saturates to 0 if x < -3.5, linear from -3.5 to -1.2, and 5th degree taylor above -1.2
      val expo = SRAM[T](1024)
      Foreach(1024 by 1){ i =>
        val x = (i.as[T] - 512) / 128
        expo(i) = exp_taylor(x)
      }
      // Test sqrt_approx from 0 to 1024
      // NOTE: This does a 3rd degree taylor centered at 1 if x < 2, and then linearizes for every order of magnitude after that
      val sqroot = SRAM[T](512)
      Foreach(512 by 1){ i =>
        sqroot(i) = sqrt_approx(i.as[T]*50 + 5)
      }
      // Test sin and cos from 0 to 2pi
      // NOTE: These do an amazing job if phi is inside +/- pi/2
      val sin = SRAM[T](1024)
      val cos = SRAM[T](1024)
      Foreach(1024 by 1){ i =>
        val phi = TWOPI*(i.as[T] / 1024) - TWOPI/2
        val beyond_left = phi < -TWOPI/4
        val beyond_right = phi > TWOPI/4
        val phi_shift = mux(beyond_left, phi + TWOPI/2, mux(beyond_right, phi - TWOPI/2, phi))
        cos(i) = -cos_taylor(phi_shift) * mux(beyond_left || beyond_right, -1.to[T], 1)
        sin(i) = -sin_taylor(phi_shift) * mux(beyond_left || beyond_right, -1.to[T], 1)
      }
      sin_dram store sin
      cos_dram store cos
      expo_dram store expo
      sqroot_dram store sqroot

      y(0 :: N par 16) store yy
    }


    // Extract results from accelerator
    val result = getMem(y)

    // Create validation checks and debug code
    val gold = x_data.map{ dat => dat * b }
    printArray(gold, "expected: ")
    printArray(result, "got: ")

    val expo_gold = Array.tabulate(1024){ i => exp(((i.to[Float])-512)/128) }
    val expo_got = getMem(expo_dram)
    printArray(expo_gold, "e^x gold: ")
    printArray(expo_got, "e^x taylor: ")

    val sin_gold = Array.tabulate(1024){ i => sin(TWOPI.to[Float]*((i.to[Float])/1024.to[Float])) }
    val sin_got = getMem(sin_dram)
    printArray(sin_gold, "sin gold: ")
    printArray(sin_got, "sin taylor: ")

    val cos_gold = Array.tabulate(1024){ i => cos(TWOPI.to[Float]*((i.to[Float])/1024.to[Float])) }
    val cos_got = getMem(cos_dram)
    printArray(cos_gold, "cos gold: ")
    printArray(cos_got, "cos taylor: ")

    val sqroot_gold = Array.tabulate(512){ i => sqrt((i.to[Float])*50 + 5) }
    val sqroot_got = getMem(sqroot_dram)
    printArray(sqroot_gold, "sqroot gold: ")
    printArray(sqroot_got, "sqroot taylor: ")
    // printArray(expo_gold.zip(expo_got){_-_.as[Float]}, "e^x error: ")
    // printArray(expo_gold.zip(expo_got){_.as[T]-_}, "e^x error: ")

    assert(gold == result)
  }
}


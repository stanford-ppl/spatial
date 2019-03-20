package spatial.tests.feature.transfers

import spatial.dsl._

@spatial class SmallTensorLoad extends SpatialTest { // This test stresses the unaligned load similar to a multichannel convolution which used to fail because the unaligned fifo in the accel
  override def dseModelArgs: Args = "16 8 16 8"
  override def finalModelArgs: Args = "16 8 16 8"
  override def runtimeArgs: Args = "8 16"
  type T = FixPt[TRUE,_16,_0]

  def main(args: Array[String]): Void = {
    val debug: scala.Boolean = false

    // Scalar params
    val INPUT_CHANS = ArgIn[I32]
    val OUTPUT_CHANS = ArgIn[I32]

    // Shadow params (input args)
    val input_chans = args(0).to[I32]
    val output_chans = args(1).to[I32]

    // Set args
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)

    // HW Design properties
    val INPUT_CHANS_MAX = 64
    val OUTPUT_CHANS_MAX = 64

    // Memories
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, 3, 3)

    // Load data (placeholder)
    val kernel = (0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::3, 0::3) {(i,j,k,l) => (l + k * 3 + j * 3 * 3 + i * 3 * 3 * INPUT_CHANS).to[T]} //if (random[I32](10) > 8) 1.to[T] else 0.to[T]}

    // Debug hooks
    val KERNEL_COPY = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, 3, 3)
    val KERNEL_COPY_CPU = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, 3, 3)

    // Set data
    setMem(KERNEL_DATA, kernel)

    setMem(KERNEL_COPY_CPU, kernel)

    Accel{
      val kernel_sram = SRAM[T](OUTPUT_CHANS_MAX, INPUT_CHANS_MAX, 3, 3)
      kernel_sram load KERNEL_DATA(0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::3, 0::3)

      KERNEL_COPY(0::OUTPUT_CHANS, 0::INPUT_CHANS, 0::3, 0::3) store kernel_sram
    }
    printTensor4(kernel, "Kernel")
    val cksum = getTensor4(KERNEL_DATA).flatten.zip(getMem(KERNEL_COPY)){_==_}.reduce{_&&_}

    printTensor4(getTensor4(KERNEL_COPY), "Kernel copied from fpga:")
    printTensor4(getTensor4(KERNEL_COPY_CPU), "Kernel copied from CPU:")

    println("PASS: " + cksum + " (SmallTensorLoad)")
    assert(cksum)
  }
}

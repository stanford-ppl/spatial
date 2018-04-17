import spatial.dsl._

@spatial object SmallTensorLoad { // This test stresses the unaligned load similar to a multichannel convolution which used to fail because the unaligned fifo in the accel
  type T = FixPt[TRUE,_16,_0]

  def main(args: Array[String]): Void = {

    // Scalar params
    val INPUT_CHANS = ArgIn[I32]
    val OUTPUT_CHANS = ArgIn[I32]

    // Shadow params (input args)
    val input_chans = args(0).to[I32]
    val output_chans = args(1).to[I32]

    // Set args
    setArg(INPUT_CHANS, input_chans)
    setArg(OUTPUT_CHANS, output_chans)

    // Memories
    val KERNEL_DATA = DRAM[T](OUTPUT_CHANS, INPUT_CHANS, 3, 3)
  }
}

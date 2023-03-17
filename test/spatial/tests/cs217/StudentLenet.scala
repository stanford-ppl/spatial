package spatial.tests.cs217

import spatial.dsl._

@spatial class StudentLenet extends SpatialTest {

  // This sets the input offset to run starting from example 0.
  override def runtimeArgs = "0"

  type T = FixPt[TRUE, _10, _22]

  // Feel free to reduce this for testing, but for final evaluation we'll be using 8
  val NumInferences = 8

  override def main(args: Array[String]): Unit = {
    val inputOffset = ArgIn[I32]
    setArg(inputOffset, args(0).to[I32])

    val input = loadCSV1D[T](s"$DATA/input.csv")
    val input_DRAM = DRAM[T](100, 28, 28)
    setMem(input_DRAM, input)

    val conv1_weights = loadCSV1D[T](s"$DATA/conv1/weights.csv")
    val conv1_kern_DRAM    = DRAM[T](5, 5, 32)
    setMem(conv1_kern_DRAM, conv1_weights)

    val conv1_biases = loadCSV1D[T](s"$DATA/conv1/biases.csv")
    val conv1_bias_DRAM   = DRAM[T](32)
    setMem(conv1_bias_DRAM, conv1_biases)

    val conv2_weights = loadCSV1D[T](s"$DATA/conv2/weights.csv")
    val conv2_kern_DRAM    = DRAM[T](5, 5, 32, 64)
    setMem(conv2_kern_DRAM, conv2_weights)

    val conv2_biases = loadCSV1D[T](s"$DATA/conv2/biases.csv")
    val conv2_bias_DRAM   = DRAM[T](64)
    setMem(conv2_bias_DRAM, conv2_biases)

    val fc3_weights = loadCSV1D[T](s"$DATA/fc3/weights.csv")
    val fc3_weight_DRAM  = DRAM[T](3136, 1024)
    setMem(fc3_weight_DRAM, fc3_weights)

    val fc3_biases = loadCSV1D[T](s"$DATA/fc3/biases.csv")
    val fc3_bias_DRAM   = DRAM[T](1024)
    setMem(fc3_bias_DRAM, fc3_biases)

    val fc4_weights = loadCSV1D[T](s"$DATA/fc4/weights.csv")
    val fc4_weight_DRAM  = DRAM[T](1024, 10)
    setMem(fc4_weight_DRAM, fc4_weights)

    val fc4_biases = loadCSV1D[T](s"$DATA/fc4/biases.csv")
    val fc4_bias_DRAM   = DRAM[T](10)
    setMem(fc4_bias_DRAM, fc4_biases)
    val output_DRAM  = DRAM[T](NumInferences, 10)

    Accel {
      val in_act = SRAM[T](NumInferences, 32, 32)
      Foreach(NumInferences by 1, 32 by 1, 32 by 1) { (infer, i,j) => in_act(infer, i, j) = 0 }

      // Weights and biases
      val w1 = SRAM[T](5, 5, 32)
      val b1 = SRAM[T](32)
      w1 load conv1_kern_DRAM(0 :: 5, 0 :: 5, 0 :: 32)
      b1 load conv1_bias_DRAM

      val w2 = SRAM[T](5, 5, 32, 64)
      val b2 = SRAM[T](64)
      w2 load conv2_kern_DRAM(0 :: 5, 0 :: 5, 0 :: 32, 0 :: 64)
      b2 load conv2_bias_DRAM

      val w3 = SRAM[T](3136, 1024)
      w3 load fc3_weight_DRAM
      val f3_bias = SRAM[T](1024)
      f3_bias load fc3_bias_DRAM
      val w4 = SRAM[T](1024, 10)
      w4 load fc4_weight_DRAM
      val f4_bias = SRAM[T](10)
      f4_bias load fc4_bias_DRAM

      // Conv1
      val offset = inputOffset.value
      in_act(0::NumInferences, 2::30, 2::30) load input_DRAM(offset::(offset + NumInferences), 0::28, 0::28)

      println(s"Data Loading Done!")

      Sequential.Foreach(0 until NumInferences) {
        infer =>
          // Conv 1 start
          val c1_out = SRAM[T](28, 28, 32)
          Foreach(28 by 1, 28 by 1, 32 by 1) { (i, j, k) =>
            val accum = Reg[T](0)
            Reduce(accum)(5 by 1, 5 by 1) { (ii, jj) =>
              w1(ii, jj, k) * in_act(infer, i + ii, j + jj)
            } {
              _ + _
            }
            c1_out(i, j, k) = accum.value + b1(k)
          }

          // End of Conv 1
          println("Conv1 Done")

          // Maxpool after Conv1
          val c1_act = SRAM[T](18, 18, 32)
          Foreach(18 by 1, 18 by 1, 32 by 1) { (i, j, k) => c1_act(i, j, k) = 0 }
          Foreach(0 until 14, 0 until 14, 0 until 32) {
            (outi, outj, k) =>
              c1_act(outi + 2, outj + 2, k) = Reduce(Reg[T](0))(2 by 1, 2 by 1) {
                (shiftI, shiftJ) =>
                  c1_out(2*outi + shiftI, 2*outj + shiftJ, k)
              } {
                (a, b) => max(a, b)
              }
          }

          println(s"MaxPool1 Done!")

          // Conv2 Start
          val c2_conv = SRAM[T](14, 14, 64)
          Foreach(14 by 1, 14 by 1, 64 by 1) { (i, j, k) =>
            val accum = Reg[T](0)
            Reduce(accum)(5 by 1, 5 by 1, 32 by 1) { (ii, jj, kk) =>
              w2(ii, jj, kk, k).to[T] * c1_act(i + ii, j + jj, kk)
            } {
              _ + _
            }
            c2_conv(i, j, k) = accum + b2(k)
//            c2_act(i / 2, j / 2, k) = max(c2_act(i / 2, j / 2, k), accum + b2(k))
          }

          // Conv 2 end
          println(s"Conv2 Done!")

          // MaxPool after Conv 2
          val c2_out = SRAM[T](7, 7, 64)
          Foreach(7 by 1, 7 by 1, 64 by 1) {
            (i, j, k) => c2_out(i, j, k) = 0
          }
          Foreach(0 until 7, 0 until 7, 0 until 64) {
            (outi, outj, k) =>
              c2_out(outi, outj, k) = Reduce(Reg[T](0))(2 by 1, 2 by 1) {
                (shiftI, shiftJ) =>
                  c2_conv(2*outi + shiftI, 2*outj + shiftJ, k)
              }{ (a, b) => max(a, b) }
          }

          println("MaxPool2 Done!")

          // FC3
          val f3_act = SRAM[T](1024)
          Foreach(0 until 1024) {
            l =>
              val accum = Reg[T](0)
              Reduce(accum)(7 by 1, 7 by 1, 2 by 1, 32 by 1) {
                case Seq(i, j, kk, k) =>
                  c2_out(i, j, kk * 32 + k) * w3(448 * i + 64 * j + (kk * 32) + k, l)
              } {
                _ + _
              }
              f3_act(l) = max(0.to[T], accum.value + f3_bias(l))
          }

          println(s"FC3 Done!")

          // FC4
          val f4_act = SRAM[T](10)
          MemReduce(f4_act)(1024 by 1) {
            i =>
              val tmp = SRAM[T](10)
              Foreach(0 until 10) {
                j =>
                  tmp(j) = w4(i, j) * f3_act(i)
              }
              tmp
          } {
            _ + _
          }
          Foreach(10 by 1) {
            i => f4_act(i) = f4_act(i) + f4_bias(i)
          }
          output_DRAM(infer, 0::10) store f4_act
          println(s"FC4 Done!")
      }
    }
    writeCSV2D(getMatrix(output_DRAM), s"$DATA/output.csv")
    assert(Bit(true))
  }
}

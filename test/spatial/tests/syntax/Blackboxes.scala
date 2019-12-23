package spatial.tests.syntax

import spatial.dsl._

/** Example usage of a primitive blackbox.  The input and output types must be defined as @structs.
  * You must provide the full path to the verilog file, the latency of the box (cycle count from input value change to output value change),
  * and its pipeline factor (i.e. number of cycles before a new input can be accepted).  Verilog module params can be passed in a Map.
  */
class VerilogPrimitiveBBox extends VerilogPrimitive(true)
class VerilogPrimitiveInline extends VerilogPrimitive(false)

@spatial abstract class VerilogPrimitive(usebox: scala.Boolean) extends SpatialTest {
  override def runtimeArgs: Args = "3"

  @struct case class BBOX_IN(in1: Int, in2: Int)
  @struct case class BBOX_OUT(out: Int)
  @struct case class BBOX_DM_OUT(div: Int, mod: Int)

  def main(args: Array[String]): Unit = {
    val a = args(0).to[Int]
    val in1 = ArgIn[Int]
    val sum = DRAM[Int](16)
    val prod = DRAM[Int](16)
    val mod = DRAM[Int](16)
    val div = DRAM[Int](16)
    val delay = DRAM[Int](16)
    setArg(in1, a)

    Accel {
      val addsram = SRAM[Int](16)
      val mulsram = SRAM[Int](16)
      val divsram = SRAM[Int](16)
      val modsram = SRAM[Int](16)
      val delaysram = SRAM[Int](16)
      Foreach(16 by 1 par 2) { i =>
        addsram(i) =
          if (usebox) Blackbox.VerilogPrimitive[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/adder.v", latency = 3, pipelineFactor = 1, params = Map("LATENCY" -> 3)).out
          else i + in1.value
        mulsram(i) =
          if (usebox) Blackbox.VerilogPrimitive[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/multiplier.v", latency = 8, pipelineFactor = 1, params = Map("LATENCY" -> 8)).out
          else i * in1.value
        val dm =
          if (usebox) Blackbox.VerilogPrimitive[BBOX_IN,BBOX_DM_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/divmodder.v", latency = 15, pipelineFactor = 1, params =  Map("LATENCY" -> 15))
          else BBOX_DM_OUT(i / in1.value, i % in1.value)
        modsram(i) = dm.mod
        divsram(i) = dm.div
        delaysram(i) =
          if (usebox) Blackbox.VerilogPrimitive[BBOX_IN,BBOX_OUT](BBOX_IN(i, 0))(s"$DATA/verilogboxes/nopipeline.v", Some("delayer"), latency = 5, pipelineFactor = 2, params = Map("LATENCY" -> 5)).out
          else retime(1,i) | i // There's not actually a way to write Spatial that forces II = 2 the way that the raw verilog does it
      }
      sum store addsram
      prod store mulsram
      mod store modsram
      div store divsram
      delay store delaysram
    }

    printArray(getMem(sum), "got sum:" )
    val sumgold = Array.tabulate(16){i => i + a}
    printArray(sumgold, "wanted sum:")
    println(r"OK: ${sumgold == getMem(sum)}")
    assert(sumgold == getMem(sum))

    printArray(getMem(prod), "got prod:" )
    val prodgold = Array.tabulate(16){i => i * a}
    printArray(prodgold, "wanted prod:")
    println(r"OK: ${prodgold == getMem(prod)}")
    assert(prodgold == getMem(prod))

    printArray(getMem(mod), "got mod:" )
    val modgold = Array.tabulate(16){i => i % a}
    printArray(modgold, "wanted mod:")
    println(r"OK: ${modgold == getMem(mod)}")
    assert(modgold == getMem(mod))

    printArray(getMem(div), "got div:" )
    val divgold = Array.tabulate(16){i => i / a}
    printArray(divgold, "wanted div:")
    println(r"OK: ${divgold == getMem(div)}")
    assert(divgold == getMem(div))

    printArray(getMem(delay), "got delay:" )
    val delaygold = Array.tabulate(16){i => i}
    printArray(delaygold, "wanted delay:")
    println(r"OK: ${delaygold == getMem(delay)}")
    assert(delaygold == getMem(delay))
  }

}


/** Example usage of a Spatial-defined primitive blackbox.  The input and output types must be defined as @structs.
  * Compile-time params to blackbox not currently supported but can be added if necessary. Compile time params are not really
  * required though because ideally the compiler should apply optimizations to the bbox contents, and having parameters
  * may result in different instantiations of the blackbox requiring different optimizations, and therefore you end up
  * with two separate modules anyway (i.e. you may as well just use function calls instead of blackboxes).
  */
class SpatialPrimitiveBBox extends SpatialPrimitive(true)
class SpatialPrimitiveInline extends SpatialPrimitive(false)

@spatial abstract class SpatialPrimitive(usebox: scala.Boolean) extends SpatialTest {
  override def runtimeArgs: Args = "2"

  @struct case class BBOX_IN(idx: Int, scalar: I32, in0: I32, in1: I32, in2: I32, in3: I32, in4: I32, in5: I32)
  @struct case class BBOX_OUT(max_idx: Int, max_val: I32, min_idx: Int, min_val: I32)

  def main(args: Array[String]): Unit = {
    val a = args(0).to[I32]
    val in1 = ArgIn[I32]
    val max_idx = DRAM[Int](8)
    val max_val = DRAM[I32](8)
    val min_idx = DRAM[Int](8)
    val min_val = DRAM[I32](8)
    setArg(in1, a)

    // Find idx where pow3 of in0-5 add up to the largest value and the smallest value
    def minmax(idx: Int, scalar: I32, in0: I32, in1: I32, in2: I32, in3: I32, in4: I32, in5: I32): (Int, I32, Int, I32) = {
      val max_val_reg = Reg[I32](0).conflictable
      val max_idx_reg = Reg[Int](-1).conflictable
      val min_val_reg = Reg[I32](0).conflictable
      val min_idx_reg = Reg[Int](-1).conflictable

      // This function just makes the blackbox body have lots more nodes unless if you choose one of these random values for your input :)
      def randomOps(x: I32): I32 = {
        if (scalar == 430978) x / (x+1) + x + scalar
        else if (scalar == 130876) x - x * x + scalar * x
        else if (scalar == 38756114) x % 71 - x + scalar * x
        else x * x * x
      }

      val sum = scalar * (randomOps(in0) + randomOps(in1) + randomOps(in2) + randomOps(in3) + randomOps(in4) + randomOps(in5))
      if (idx == 0 || max_val_reg.value < sum) {
        max_val_reg:= sum
        max_idx_reg:= idx
      }
      if (idx == 0 || min_val_reg.value > sum) {
        min_val_reg:= sum
        min_idx_reg:= idx
      }
      (max_idx_reg.value, max_val_reg.value, min_idx_reg.value, min_val_reg.value)
    }

    // Must declare blackboxes outside of Accel
    val minMaxBBox = Blackbox.SpatialPrimitive[BBOX_IN, BBOX_OUT]{ in: BBOX_IN =>
      // Specify how this bbox converts a BBOX_IN value to a BBOX_OUT value (we just borrow the minmax function here)
      val (max_idx, max_val, min_idx, min_val) = minmax(in.idx, in.scalar, in.in0, in.in1, in.in2, in.in3, in.in4, in.in5)
      BBOX_OUT(max_idx, max_val, min_idx, min_val)
    }

    Accel {
      val max_idx_sram = SRAM[Int](8)
      val max_val_sram = SRAM[I32](8)
      val min_idx_sram = SRAM[Int](8)
      val min_val_sram = SRAM[I32](8)

      Foreach(8 by 1 par 4) { i =>
        val rowOfs = i * 2 // For noisifying the generated data and make the correctness check easy
        val range = i + 10
        val center = range/2
        Foreach(64 by 1){ j =>
          val data = List.tabulate[I32](6){k =>
            // Include spikes to make it easy to check for correctness
            val pos = j + k
            if (pos > 12 - rowOfs && pos < 15 - rowOfs) 12.to[I32]
            else if (pos > 21 - rowOfs && pos < 28 - rowOfs) (24 + rowOfs).to[I32]
            else if (pos > 30 - rowOfs && pos < 34 - rowOfs) -12.to[I32]
            else if (pos > 37 - rowOfs && pos < 44 - rowOfs) (-24 + rowOfs).to[I32]
            else (pos % range - center).to[I32]
          }

          // Using blackbox
          if (usebox) {
            val bbox = minMaxBBox(BBOX_IN(j, in1.value, data(0), data(1), data(2), data(3), data(4), data(5)))
            if (j == 63.to[Int]) {
              max_idx_sram(i) = bbox.max_idx
              max_val_sram(i) = bbox.max_val
              min_idx_sram(i) = bbox.min_idx
              min_val_sram(i) = bbox.min_val
            }
          } else {
            // Using Inline function
            val (max_idx, max_val, min_idx, min_val) = minmax(j, in1.value, data(0), data(1), data(2), data(3), data(4), data(5))
            if (j == 63.to[Int]) {
              max_idx_sram(i) = max_idx
              max_val_sram(i) = max_val
              min_idx_sram(i) = min_idx
              min_val_sram(i) = min_val
            }
          }
        }
      }
      max_idx store max_idx_sram
      max_val store max_val_sram
      min_idx store min_idx_sram
      min_val store min_val_sram
    }

    printArray(getMem(max_idx), "got max_idx:" )
    val max_idx_gold = Array.tabulate(8){i => 1 + 21 - i*2}
    printArray(max_idx_gold, "wanted max_idx:")
    println(r"OK: ${max_idx_gold == getMem(max_idx)}")
    assert(max_idx_gold == getMem(max_idx))

    printArray(getMem(max_val), "got max_val:" )
    val max_val_gold = Array.tabulate(8){i => ((24 + 2*i) * (24 + 2*i) * (24 + 2*i)*6*a).to[I32]}
    printArray(max_val_gold, "wanted max_val:")
    println(r"OK: ${max_val_gold == getMem(max_val)}")
    assert(max_val_gold == getMem(max_val))

    printArray(getMem(min_idx), "got min_idx:" )
    val min_idx_gold = Array.tabulate(8){i => 1 + 37 - i*2}
    printArray(min_idx_gold, "wanted min_idx:")
    println(r"OK: ${min_idx_gold == getMem(min_idx)}")
    assert(min_idx_gold == getMem(min_idx))

    printArray(getMem(min_val), "got min_val:" )
    val min_val_gold = Array.tabulate(8){i => ((-24 + i.to[I32]*2)*(-24 + i.to[I32]*2)*(-24 + i.to[I32]*2)*6*a).to[I32]}
    printArray(min_val_gold, "wanted min_val:")
    println(r"OK: ${min_val_gold == getMem(min_val)}")
    assert(min_val_gold == getMem(min_val))

  }
}


/** Example usage of a Spatial-defined controller blackbox.  The input and output types must be defined as @streamstructs.
  * Compile-time params to blackbox not currently supported but can be added if necessary.  Compile time params are not really
  * required though because ideally the compiler should apply optimizations to the bbox contents, and having parameters
  * may result in different instantiations of the blackbox requiring different optimizations, and therefore you end up
  * with two separate modules anyway (i.e. you may as well just use function calls instead of blackboxes).
  */
class SpatialCtrlBBox extends SpatialCtrl(true)
class SpatialCtrlInline extends SpatialCtrl(false)

@spatial abstract class SpatialCtrl(usebox: scala.Boolean) extends SpatialTest {
  override def compileArgs = super.compileArgs and "--noModifyStream"
  @streamstruct case class BBOX_IN(numel: Int, scalar: I16)
  @streamstruct case class BBOX_OUT(index: Int, payload: I16, numelOut: Int)

  def main(args: Array[String]): Unit = {
    val numInstantiations = 2
    val result = List.fill(numInstantiations) (DRAM[I16](4,64))

    // Define bbox outside of Accel
    val bboxImpl = Blackbox.SpatialController[BBOX_IN, BBOX_OUT] { in: BBOX_IN =>
      val index = FIFO[Int](8)
      val payload = FIFO[I16](8)
      val numelOut = FIFO[Int](8)
      Foreach(4 by 1) { _ =>
        val N = in.numel
        val sc = in.scalar
        val counterReg = Reg[Int]
        Reduce(counterReg)(N by 1) { i =>
          mux(i == 357861, 3112, 2) // Random stuff
        }{_+_}
        Foreach(counterReg by 1) { i =>
          index.enq(i)
          payload.enq(i.to[I16] * sc)
          if (i == 0) numelOut.enq(N * 2)
        }
      }
      BBOX_OUT(index.deqInterface(), payload.deqInterface(), numelOut.deqInterface())
    }

    Accel {
      val sram = List.fill(numInstantiations) (SRAM[I16](4,64))
      Foreach(sram.head.rows by 1, sram.head.cols by 1){(i,j) => sram.foreach{s => s(i,j) = 0.to[I16]}}
      Stream.Foreach(5 by 1){ _ =>
//      Stream{
        val numel = List.fill(numInstantiations)(FIFO[Int](8))
        val scalar = List.fill(numInstantiations)(FIFO[I16](8))
        Foreach(4 by 1) { row =>
          numel.foreach(_.enq(row * 4 + 16))
          scalar.foreach(_.enq(row.to[I16] + 1))
        }

        if (usebox) {
          val bbox = List.tabulate(numInstantiations){lane => bboxImpl(BBOX_IN(numel(lane).deqInterface(), scalar(lane).deqInterface()))}
          List.tabulate(numInstantiations) { lane =>
            Foreach(4 by 1) { row =>
              val numel = bbox(lane).numelOut
              Foreach(numel by 1) { i =>
                sram(lane)(row, bbox(lane).index) = bbox(lane).payload
              }
            }
          }
          0
        } else {
          List.tabulate(numInstantiations){ lane =>
            val index = FIFO[Int](8)
            val payload = FIFO[I16](8)
            val numelOut = FIFO[Int](8)
            'BBOX.Pipe {
              Foreach(4 by 1) { _ =>
                val N = numel(lane).deq()
                val sc = scalar(lane).deq()
                val counterReg = Reg[Int]
                Reduce(counterReg)(N by 1) { i =>
                  mux(i == 357861, 3112, 2) // Random stuff
                }{_+_}
                Foreach(counterReg by 1) { i =>
                  index.enq(i)
                  payload.enq(i.to[I16] * sc)
                  if (i == 0) numelOut.enq(N * 2)
                }
              }
            }
            Foreach(4 by 1) { row =>
              val N = numelOut.deq()
              Foreach(N by 1) { i =>
                sram(lane)(row, index.deq()) = payload.deq()
              }
            }
          }
          0
        }
      }

      result.zip(sram).foreach{case (d,s) => d store s}
    }

    val gold = (0::4,0::64){(i,j) =>
      val scalar = (i + 1).to[I16]
      val numel = (i * 4 + 16)*2
      if (j < numel) (j.to[I16] * scalar) else 0.to[I16]
    }

    List.tabulate(numInstantiations) { lane =>
      printMatrix(getMatrix(result(lane)), "got:")
      printMatrix(gold, "wanted:")
      println(r"OK: ${gold == getMatrix(result(lane))}}")
      assert(gold == getMatrix(result(lane)))
    }
    ()
  }
}

/** Example usage of a verilog blackbox as a controller.  You must create a @streamstruct to represent the input and output types.
  * The verilog must have enable, done, reset, and clock signals, and these are automatically wired up by the compiler.
  * Each field of the streamstruct must respect ready/valid handshaking (i.e. dequeue occurs during cycle when both are high).
  * The blackbox should probably have some small FIFOs to handle the output streamstruct.
  * You must provide the full path to the verilog file.  Verilog module params can be passed in a Map.
  */
class VerilogCtrlBBox extends VerilogCtrl(true)
class VerilogCtrlInline extends VerilogCtrl(false)

@spatial abstract class VerilogCtrl(usebox: scala.Boolean) extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--noModifyStream"
  @streamstruct case class BBOX_IN(numel: Int, scalar: I16)
  @streamstruct case class BBOX_OUT(index: Int, payload: I16, numelOut: Int)

  def main(args: Array[String]): Unit = {

    val numInstantiations = 2
    val result = List.fill(numInstantiations) (DRAM[I16](4,64))

    Accel {
      val sram = List.fill(numInstantiations) (SRAM[I16](4,64))
      Foreach(sram.head.rows by 1, sram.head.cols by 1){(i,j) => sram.foreach{s => s(i,j) = 0.to[I16]}}
      Stream.Foreach(5 by 1){ _ =>
//      Stream{
        val numel = List.fill(numInstantiations)(FIFO[Int](8))
        val scalar = List.fill(numInstantiations)(FIFO[I16](8))
        Foreach(4 by 1) { row =>
          numel.foreach(_.enq(row * 4 + 16))
          scalar.foreach(_.enq(row.to[I16] + 1))
        }

        if (usebox) {
          val bbox = List.tabulate(numInstantiations){lane => Blackbox.VerilogController[BBOX_IN, BBOX_OUT](BBOX_IN(numel(lane).deqInterface(), scalar(lane).deqInterface()))(s"$DATA/verilogboxes/ctrl.v", params = Map("WIDTH" -> 16))}

          List.tabulate(numInstantiations){lane =>
            Foreach(4 by 1) { row =>
              val numel = bbox(lane).numelOut
              Foreach(numel by 1) { i =>
                sram(lane)(row, bbox(lane).index) = bbox(lane).payload
              }
            }
          }
          0
        } else {
          List.tabulate(numInstantiations) { lane => {
            val index = FIFO[Int](8)
            val payload = FIFO[I16](8)
            val numelOut = FIFO[Int](8)
            'BBOX.Pipe {
              Foreach(4 by 1) { _ =>
                val N = numel(lane).deq()
                val sc = scalar(lane).deq()
                Foreach(N * 2 by 1) { i =>
                  index.enq(i)
                  payload.enq(i.to[I16] * sc)
                  if (i == 0) numelOut.enq(N * 2)
                }
              }
            }
            Foreach(4 by 1) { row =>
              val N = numelOut.deq()
              Foreach(N by 1) { i =>
                sram(lane)(row, index.deq()) = payload.deq()
              }
            }
          }}
          0
        }
      }

      result.zip(sram).foreach{case (d,s) => d store s}
    }

    val gold = (0 :: 4, 0 :: 64) { (i, j) =>
      val scalar = (i + 1).to[I16]
      val numel = (i * 4 + 16) * 2
      if (j < numel) (j.to[I16] * scalar) else 0.to[I16]
    }
    List.tabulate(numInstantiations) { lane =>
      printMatrix(getMatrix(result(lane)), "got:")
      printMatrix(gold, "wanted:")
      println(r"OK: ${gold == getMatrix(result(lane))}")
      assert(gold == getMatrix(result(lane)))
    }
    ()
  }
}

/** Rip the verilog out of this app and doctor it a bit to serve as the bbox for the VerilogCtrlBBox app.
  *  This is not the intended use case for using verilogCtrlBlackboxes but it is one hacky way of doing it.
  *  Ideally, the programmer comes in with their own verilog (handwritten or from another compiler),
  *  makes sure the interface meets the Spatial assumptions and plugs it in.  Let's pretend this app is a stand-in for HLS or something.
  *  Compile the backend with `export FRINGELESS=1`, rip out the BBOX module, and wrap it in the interface Spatial assumes for
  *  ctrl bboxes
  */
@spatial class CtrlBBoxGenerator extends SpatialTest {

  def main(args: Array[String]): Unit = {
    Accel{
      // This loop takes a number of desired elements and a scalar factor and generates index/value pairs to be consumed downstream
      Stream{
        val numel = FIFO[Int](8)
        val scalar = FIFO[I16](8)
        val index = FIFO[Int](8)
        val payload = FIFO[I16](8)
        val numelOut = FIFO[Int](8)
        Pipe{
          numel.enq(1)
          scalar.enq(1.to[I16])
        }
        'BBOX.Pipe{
          Foreach(4 by 1) { _ =>
            val N = numel.deq()
            val sc = scalar.deq()
            Foreach(N * 2 by 1) { i =>
              index.enq(i)
              payload.enq(i.to[I16] * sc)
              if (i == 0) numelOut.enq(N * 2)
            }
          }
        }
        Pipe{
          println(r"${index.deq()}")
          println(r"${payload.deq()}")
          println(r"${numelOut.deq()}")
        }
      }
    }
  }
}
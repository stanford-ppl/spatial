package spatial.tests.syntax

import spatial.dsl._

/** Example usage of a primitive blackbox.  The input and output types must be defined as @structs.
  * You must provide the full path to the verilog file, the latency of the box (cycle count from input value change to output value change),
  * and whether it is pipelined.  Partial pipelining (i.e. can accept a new value every n cycles, where n > 1 and n < latency) is not
  * currently supported but would be easy to add if needed.  Verilog module params can be passed in a Map.
  */
@spatial class VerilogPrimitiveBBox extends SpatialTest {
  override def runtimeArgs: Args = "3"

  @struct case class BBOX_IN(in1: Int, in2: Int)
  @struct case class BBOX_OUT(out: Int)
  @struct case class BBOX_DM_OUT(div: Int, mod: Int)

  def main(args: Array[String]): Unit = {
    val a = args(0).to[Int]
    val b = args(1).to[Int]
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
      Foreach(16 by 1) { i =>
        addsram(i) = verilogPrimitiveBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/adder.v", latency = 3, pipelined = true, params = Map("LATENCY" -> 3)).out
        mulsram(i) = verilogPrimitiveBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/multiplier.v", latency = 8, pipelined = true, params = Map("LATENCY" -> 8)).out
        val dm = verilogPrimitiveBlackBox[BBOX_IN,BBOX_DM_OUT](BBOX_IN(i, in1.value))(s"$DATA/verilogboxes/divmodder.v", latency = 15, pipelined = true, params =  Map("LATENCY" -> 15))
        modsram(i) = dm.mod
        divsram(i) = dm.div
        delaysram(i) = verilogPrimitiveBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(i, 0))(s"$DATA/verilogboxes/nopipeline.v", latency = 2, pipelined = false, params = Map("LATENCY" -> 2)).out
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

/** Example usage of a verilog blackbox as a controller.  You must create a @streamstruct to represent the input and output types.
  * The verilog must have enable, done, reset, and clock signals, and these are automatically wired up by the compiler.
  * Each field of the streamstruct must respect ready/valid handshaking (i.e. dequeue occurs during cycle when both are high).
  * The blackbox should probably have some small FIFOs to handle the output streamstruct.
  * You must provide the full path to the verilog file.  Verilog module params can be passed in a Map.
  */
@spatial class VerilogCtrlBBox extends SpatialTest {

  @streamstruct case class BBOX_IN(numel: Int, scalar: I16)
  @streamstruct case class BBOX_OUT(index: Int, payload: I16, numelOut: Int)

  def main(args: Array[String]): Unit = {

    val result = DRAM[I16](4,64)

    Accel {
      val sram = SRAM[I16](4,64)
      Foreach(sram.rows by 1, sram.cols by 1){(i,j) => sram(i,j) = 0.to[I16]}
      Stream.Foreach(5 by 1){ _ =>
//      Stream{
        val numel = FIFO[Int](8)
        val scalar = FIFO[I16](8)
        Foreach(4 by 1) { row =>
          numel.enq(row * 4 + 16)
          scalar.enq(row.to[I16] + 1)
        }

        val bbox = verilogControllerBlackBox[BBOX_IN,BBOX_OUT](BBOX_IN(numel.deqInterface(), scalar.deqInterface()))(s"$DATA/verilogboxes/ctrl.v", params = Map("WIDTH" -> 16))

        Foreach(4 by 1){ row =>
          val numel = bbox.numelOut
          Foreach(numel by 1){ i =>
            sram(row, bbox.index) = bbox.payload
          }
        }
      }

      result store sram

    }

    printMatrix(getMatrix(result), "got:" )
    val gold = (0::4,0::64){(i,j) =>
      val scalar = (i + 1).to[I16]
      val numel = (i * 4 + 16)*2
      if (j < numel) (j.to[I16] * scalar) else 0.to[I16]
    }
    printMatrix(gold, "wanted:")
    println(r"OK: ${gold == getMatrix(result)}")
    assert(gold == getMatrix(result))

  }

}

/** The in-lined version of what the VerilogCtrlBBox app is doing, for reference
  *
  */
@spatial class InlineCtrlBBox extends SpatialTest {

  @streamstruct case class BBOX_IN(numel: Int, scalar: I16)
  @streamstruct case class BBOX_OUT(index: Int, payload: I16, numelOut: Int)

  def main(args: Array[String]): Unit = {

    val result = DRAM[I16](4,64)

    Accel {
      val sram = SRAM[I16](4,64)
      Foreach(sram.rows by 1, sram.cols by 1){(i,j) => sram(i,j) = 0.to[I16]}
      Stream.Foreach(5 by 1){ _ =>
//      Stream{
        val numel = FIFO[Int](8)
        val scalar = FIFO[I16](8)
        val index = FIFO[Int](8)
        val payload = FIFO[I16](8)
        val numelOut = FIFO[Int](8)
        Foreach(4 by 1) { row =>
          numel.enq(row * 4 + 16)
          scalar.enq(row.to[I16] + 1)
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
        Foreach(4 by 1){ row =>
          val N = numelOut.deq()
          Foreach(N by 1){ i =>
            sram(row, index.deq()) = payload.deq()
          }
        }
      }

      result store sram

    }

    printMatrix(getMatrix(result), "got:" )
    val gold = (0::4,0::64){(i,j) =>
      val scalar = (i + 1).to[I16]
      val numel = (i * 4 + 16)*2
      if (j < numel) (j.to[I16] * scalar) else 0.to[I16]
    }
    printMatrix(gold, "wanted:")
    println(r"OK: ${gold == getMatrix(result)}")
    assert(gold == getMatrix(result))

  }

}

package spatial.tests.compiler

import spatial.dsl._

@spatial class CircTestBasicPipe extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      Pipe {
        // PIPE 1
        val reg = Reg[Int](0)
        val circ = Circ((x: Int) => x + 99)
        val y1 = circ(20)
        val y2 = circ(20)

        // PIPE 2 (first use: y1)
        val x1 = y1 + 20
        reg.write(100)

        // PIPE 3 (first use: y2; forwarded: y1, x1)
        val x2 = y2 + 20
        val x3 = x1 + x2
        val x4 = x3 + y1
        val x5 = x4 + reg.value
        out := x5
      }
    }
    assert(out.value == I32(497), r"Expected 497, found: ${out.value}")
  }
}

@spatial class CircTestBasicAccel extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      // PIPE 1
      val reg = Reg[Int](0)
      val circ = Circ((x: Int) => x + 99)
      val y1 = circ(20)
      val y2 = circ(20)

      // PIPE 2 (first use: y1)
      val x1 = y1 + 20
      reg.write(100)

      // PIPE 3 (first use: y2; forwarded: y1, x1)
      val x2 = y2 + 20
      val x3 = x1 + x2
      val x4 = x3 + y1
      val x5 = x4 + reg.value
      out := x5
    }
    assert(out.value == I32(497), r"Expected 497, found: ${out.value}")
  }
}

@spatial class CircTestNested1 extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      // PIPE 1
      val mem = SRAM[Int](8)
      val circ = Circ((x: Int) => x + 99)
      val y1 = circ(20)

      // PIPE 2
      Foreach(4 by 1) { j =>
        mem(j % 2) += 1
      }

      // PIPE 3
      mem(0) = circ(20)

      // PIPE 4 (first use: y1)
      out := y1 + mem(0)
    }
    assert(out.value == I32(238), r"Expected 238, found: ${out.value}")
  }
}

@spatial class CircTestNested2 extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      // PIPE 1
      val mem = SRAM[Int](8)
      val circ1 = Circ((x: Int) => x + 99)
      val circ2 = Circ((x: Int) => x + 99)
      val y1 = circ1(20)
      val y2 = circ2(20)

      // PIPE 2
      Foreach(4 by 1) { j =>
        val x = y1 + y2
        mem(j % 2) += x
      }

      // PIPE 3
      out := mem(0)
    }
    assert(out.value == I32(476), r"Expected 476, found: ${out.value}")
  }
}

@spatial class CircTestNested3 extends SpatialTest {
  def main(args: Array[String]): Unit = {
    val out = ArgOut[Int]
    Accel {
      // PIPE 1
      val mem = SRAM[Int](8)
      val circ = Circ((x: Int) => x + 99)

      // PIPE 2
      Foreach(4 by 1) { j =>
        mem(j % 2) += circ(20)
      }

      // PIPE 3
      out := mem(0)
    }
    assert(out.value == I32(238), r"Expected 238, found: ${out.value}")
  }
}

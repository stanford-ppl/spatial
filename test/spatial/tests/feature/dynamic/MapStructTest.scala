package spatial.tests.feature.dynamic

import spatial.dsl._
import spatial.util.MapStructType

@struct case class StructElement(a: I32, b: Bit)

@spatial class MapStructTest extends SpatialTest {

  override def compileArgs = "--nostreamify --vv"

  val IntValue: scala.Int = 0x1010

  override def main(args: Array[String]) = {
    val VST = MapStructType(Seq("z" -> StructElement(I32(0), Bit(true))))
    System.out.println(s"VST: $VST")

    val zAOut = ArgOut[I32]
    val zBOut = ArgOut[Bit]

    Accel {
      val tmp = Reg[VST.MapStruct]

      Pipe {
        val packed = VST(Map("z" -> StructElement(I32(IntValue), Bit(false))))
        tmp := packed
      }
      Pipe {
        val read = tmp.value.unpack.toMap
        val z = read("z").asInstanceOf[StructElement]
        zAOut := z.a
        zBOut := z.b
      }
    }

    println(r"Received: z -> StructElement(${zAOut.value}, ${zBOut.value})")
    assert(zAOut.value == I32(IntValue))
    assert(zBOut.value == Bit(false))
  }
}

@spatial class MapStructTest2 extends SpatialTest {

  override def compileArgs = "--nostreamify --vv"

  val IntValue: scala.Int = 0x1010

  @struct case class VST(z: StructElement)

  override def main(args: Array[String]) = {

    val zAOut = ArgOut[I32]
    val zBOut = ArgOut[Bit]

    Accel {
      val tmp = Reg[VST]

      Pipe {
        val packed = VST(StructElement(I32(IntValue), Bit(false)))
        tmp := packed
      }
      Pipe {
        val read = tmp.value
        val z = read.z
        zAOut := z.a
        zBOut := z.b
      }
    }

    println(r"Received: z -> StructElement(${zAOut.value}, ${zBOut.value})")
    assert(zAOut.value == I32(IntValue))
    assert(zBOut.value == Bit(false))
  }
}


@spatial class MapStructTestStream extends SpatialTest {

  override def compileArgs = "--nostreamify --vv"

  val iters = 8

  override def main(args: Array[String]) = {
    val MST = MapStructType(Seq("x" -> Bit(true), "y" -> I32(0)))
    println(s"MapStructType: $MST")
    val out = ArgOut[I32]
    Accel {
      val tmp = FIFO[MST.MapStruct](I32(4))
      Stream {
        Foreach(iters by 1) {
          i =>
            val packed = MST(Map("x" -> ((i & I32(1)) === I32(0)), "y" -> i))
            tmp.enq(packed)
        }
        Pipe {
          out := Reduce(Reg[I32])(iters by 1) {
            i =>
              val read = tmp.deq().unpack.toMap
              mux(read("x").asInstanceOf[Bit], read("y").asInstanceOf[I32], I32(0))
          }{ _ + _ }
        }
      }
    }

    val gold = ((0 to iters-1).map {
      i => if ((i & 1) == 0) { i } else { 0 }
    }).reduceTree{_ + _}

    println(r"Received: ${out.value}, gold: ${gold}")
    assert(out.value == gold)
  }
}
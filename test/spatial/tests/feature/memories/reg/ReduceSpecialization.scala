package spatial.tests.feature.memories.reg

import spatial.dsl._

@spatial class ReduceSpecialization extends SpatialTest {
  override def compileArgs = "--optimizeReduce --fpga Zynq"


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    val data = Array.tabulate(32){i => i + 1 }
    setMem(dram, data)

    val sum  = ArgOut[Int]
    val prod = ArgOut[Int]
    val maxn = ArgOut[Int]
    val minn = ArgOut[Int]

    Accel {
      val sram = SRAM[Int](32)
      sram load dram

      val sumAccum = Reg[Int]
      sum := Reduce(sumAccum)(0 until 32){i => sram(i) }{_+_}

      val prodAccum = Reg[Int]
      prod := Reduce(prodAccum)(0 until 32){i => sram(i) }{_*_}

      val maxAccum = Reg[Int]
      maxn := Reduce(maxAccum)(0 until 32){i => sram(i) }{(a,b) => max(a,b) }

      val minAccum = Reg[Int]
      minn := Reduce(minAccum)(0 until 32){i => sram(i) }{(a,b) => min(a,b) }
    }

    val goldSum  = data.reduce(_+_)
    val goldProd = data.reduce(_*_)
    val goldMax  = data.reduce{(a,b) => max(a,b) }
    val goldMin  = data.reduce{(a,b) => min(a,b) }

    println("--- Sum ---")
    println(r"Result: $sum")
    println(r"Golden: $goldSum")
    println("--- Product --- ")
    println(r"Result: $prod")
    println(r"Golden: $goldProd")
    println("--- Max ---")
    println(r"Result: $maxn")
    println(r"Golden: $goldMax")
    println("--- Min ---")
    println(r"Result: $minn")
    println(r"Golden: $goldMin")

    assert(goldSum == sum.value)
    assert(goldProd == prod.value)
    assert(goldMax == maxn.value)
    assert(goldMin == minn.value)
  }
}

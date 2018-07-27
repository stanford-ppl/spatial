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
    val fma32 = ArgOut[Int]
    val fma8 = ArgOut[Int8]

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

      val fma32Accum = Reg[Int]
      fma32 := Reduce(fma32Accum)(0 until 32){i => sram(i) * sram(i)}{_+_}

      val fma8Accum = Reg[Int8]
      fma8 := Reduce(fma8Accum)(0 until 32){i => sram(1).to[Int8] * sram(2).to[Int8]}{_+_}
    }

    val goldSum  = data.reduce(_+_)
    val goldProd = data.reduce(_*_)
    val goldMax  = data.reduce{(a,b) => max(a,b) }
    val goldMin  = data.reduce{(a,b) => min(a,b) }
    val goldFma32 = data.map{a => a*a}.reduce{_+_}
    val goldFma8 = Array.tabulate(32){i => 2.to[Int8] * 3.to[Int8]}.reduce{_+_}

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
    println("--- FMA32 ---")
    println(r"Result: $fma32")
    println(r"Golden: $goldFma32")
    println("--- FMA8 ---")
    println(r"Result: $fma8")
    println(r"Golden: $goldFma8")

    assert(goldSum == sum.value)
    assert(goldProd == prod.value)
    assert(goldMax == maxn.value)
    assert(goldMin == minn.value)
    assert(goldFma32 == fma32.value)
    assert(goldFma8 == fma8.value)
  }
}

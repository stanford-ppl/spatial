package spatial.tests.feature.memories.reg

import argon.Block
import spatial.dsl._

@spatial class ReduceSpecialization extends SpatialTest {
  // override def compileArgs = "--fpga Zynq"


  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](32)
    val data = Array.tabulate(32){i => i + 1 }
    val data16 = Array.tabulate(32){i => i.to[Int16]}
    setMem(dram, data)

    val sum  = ArgOut[Int]
    val prod = ArgOut[Int]
    val maxn = ArgOut[Int]
    val minn = ArgOut[Int]
    val fma32 = ArgOut[Int]
    val fma8 = ArgOut[Int8]
//    val midReset = ArgOut[Int]

    val out: List[DRAM1[Int]] = List.tabulate(6){i => DRAM[Int](32) }
    val out16: List[DRAM1[Int16]] = List.tabulate(6){i => DRAM[Int16](32) }

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

      val data: List[SRAM1[Int]] = List.tabulate(6){i => SRAM[Int](32) }
      Foreach(0 until 32){i =>
        val sumAccum_Pipe = Reg[Int]
        data(0)(i) = Reduce(sumAccum_Pipe)(0 until 32){j => sram(j) + i }{_+_}
      }

      Foreach(0 until 32){i =>
        val prodAccum_Pipe = Reg[Int]
        data(1)(i) = Reduce(prodAccum_Pipe)(0 until 32){j => sram(j) + i }{_*_}
      }

      Foreach(0 until 32){i =>
        val maxAccum_Pipe = Reg[Int]
        data(2)(i) = Reduce(maxAccum_Pipe)(0 until 32){j => sram(j) * i }{(a,b) => max(a,b)}
      }

      Foreach(0 until 32){i =>
        val minAccum_Pipe = Reg[Int]
        data(3)(i) = Reduce(minAccum_Pipe)(0 until 32){j => sram(j) * i }{(a,b) => min(a,b)}
      }

      Foreach(0 until 32){i =>
        val fma32Accum_Pipe = Reg[Int]
        data(4)(i) = Reduce(fma32Accum_Pipe)(0 until 32){j => sram(j) * sram(j) * i }{_+_}
      }

      Foreach(0 until 32){i =>
        val fma8Accum_Pipe = Reg[Int8]
        data(5)(i) = Reduce(fma8Accum_Pipe)(0 until 32){j => sram(j).to[Int8] * sram(j).to[Int8] * i.to[Int8] }{_+_}.value.to[Int]
      }

      val sram16 = SRAM[Int16](32)
      Foreach(0 until 32 by 1){i => sram16(i) = i.to[Int16]}
      val data16: List[SRAM1[Int16]] = List.tabulate(1){i => SRAM[Int16](32) }
      Foreach(0 until 32){i =>
        val sumAccum_Pipe = Reg[Int16]
        data16(0)(i) = Reduce(sumAccum_Pipe)(0 until 32){j => sram16(j) + i.to[Int16] }{_+_}
      }

      // This kind of reset mid-accumulation is not well supported because it may retime the reset to later than expected
//      val resetAccum = Reg[Int]
//      Foreach(32 by 1){i =>
//        resetAccum :+= i
//        if (i == 16) resetAccum.reset();
//      }
//      midReset := resetAccum.value

      // val intermediateAccum = Reg[Float]
      // Foreach(0 until 32){i =>
      //   intermediateAccum := intermediateAccum.value + i.to[Float]
      //   println(intermediateAccum.value)
      // }

      data.zip(out).foreach{case (sramN, dramN) => dramN store sramN }
      data16.zip(out16).foreach{case (sramN, dramN) => dramN store sramN }
    }

    val goldSum  = data.reduce(_+_)
    val goldProd = data.reduce(_*_)
    val goldMax  = data.reduce{(a,b) => max(a,b) }
    val goldMin  = data.reduce{(a,b) => min(a,b) }
    val goldFma32 = data.map{a => a*a}.reduce{_+_}
    val goldFma8 = Array.tabulate(32){i => 2.to[Int8] * 3.to[Int8]}.reduce{_+_}
//    val goldMidReset = Array.tabulate(32){i => if (i <= 16) 0 else i}.reduce{_+_}

    val goldSum_Pipe  = (0 :: 32){i => goldSum + 32*i }
    val goldProd_Pipe = (0 :: 32){i => data.map(_+i).reduce{_*_} }
    val goldMax_Pipe  = (0 :: 32){i => data.map(_*i).reduce{(a,b) => max(a,b) }}
    val goldMin_Pipe  = (0 :: 32){i => data.map(_*i).reduce{(a,b) => min(a,b) }}
    val goldFma32_Pipe = (0 :: 32){i => data.map{a => a*a*i }.reduce{_+_}}
    val goldFma8_Pipe = (0 :: 32){i => data.map{a => a.to[Int8]*a.to[Int8]*i.to[Int8] }.reduce{_+_}.to[Int] }
    val goldAdd16_Pipe = (0 :: 32){i => data16.map(_+i.to[Int16]).reduce{_+_}}

    val results: List[Array[Int]] = out.map{o => getMem(o) }
    val results16: List[Array[Int16]] = out16.map{o => getMem(o) }

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
    println("--- Sum [Pipe] ---")
    println(r"Result: ${results(0)}")
    println(r"Golden: $goldSum_Pipe")
    println("--- Prod [Pipe] ---")
    println(r"Result: ${results(1)}")
    println(r"Golden: $goldProd_Pipe")
    println("--- Max [Pipe] ---")
    println(r"Result: ${results(2)}")
    println(r"Golden: $goldMax_Pipe")
    println("--- Min [Pipe] ---")
    println(r"Result: ${results(3)}")
    println(r"Golden: $goldMin_Pipe")
    println("--- FMA32 [Pipe] ---")
    println(r"Result: ${results(4)}")
    println(r"Golden: $goldFma32_Pipe")
    println("--- FMA8 [Pipe] ---")
    println(r"Result: ${results(5)}")
    println(r"Golden: $goldFma8_Pipe")
    println("--- Add16 [Pipe] ---")
    println(r"Result: ${results16(0)}")
    println(r"Golden: $goldAdd16_Pipe")
//    println("--- MidReset ---")
//    println(r"Result: ${midReset}")
//    println(r"Golden: ${goldMidReset}")

    println(r"""
Test Sum:         ${goldSum == sum.value}
Test Prod:        ${goldProd == prod.value}
Test Max:         ${goldMax == maxn.value}
Test Min:         ${goldMin == minn.value}
Test Fma32:       ${goldFma32 == fma32.value}
Test Fma8:        ${goldFma8 == fma8.value}
Test Sum_Pipe:    ${goldSum_Pipe == results(0)}
Test Prod_Pipe:   ${goldProd_Pipe == results(1)}
Test Max_Pipe:    ${goldMax_Pipe == results(2)}
Test Min_Pipe:    ${goldMin_Pipe == results(3)}
Test Fma32_Pipe:  ${goldFma32_Pipe == results(4)}
Test Fma8_Pipe:   ${goldFma8_Pipe == results(5)}
Test Add16_Pipe:   ${goldAdd16_Pipe == results16(0)}
""")
    assert(goldSum == sum.value)
    assert(goldProd == prod.value)
    assert(goldMax == maxn.value)
    assert(goldMin == minn.value)
    assert(goldFma32 == fma32.value)
    assert(goldFma8 == fma8.value)
    assert(goldSum_Pipe == results(0))
    assert(goldProd_Pipe == results(1))
    assert(goldMax_Pipe == results(2))
    assert(goldMin_Pipe == results(3))
    assert(goldFma32_Pipe == results(4))
    assert(goldFma8_Pipe == results(5))
    assert(goldAdd16_Pipe == results16(0))
//    assert(goldMidReset == midReset.value)
  }

  override def checkIR(block: Block[_]): Result = {
    import argon._
    import spatial.metadata.memory._
    import spatial.node._

    val regs = block.nestedStms.collect{case reg:Reg[_] => reg }
    val sumAccum = regs.filter(_.name.exists(_.startsWith("sumAccum")))
    val prodAccum = regs.filter(_.name.exists(_.startsWith("prodAccum")))
    val maxAccum = regs.filter(_.name.exists(_.startsWith("maxAccum")))
    val minAccum = regs.filter(_.name.exists(_.startsWith("minAccum")))
    val fma32Accum = regs.filter(_.name.exists(_.startsWith("fma32Accum")))
    val fma8Accum  = regs.filter(_.name.exists(_.startsWith("fma8Accum")))
    require(sumAccum.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumAdd; case _ => false }}, "Sum specialization (Int)")
    require(prodAccum.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMul; case _ => false }}, "Product specialization (Int)")
    require(maxAccum.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMax; case _ => false }}, "Max specialization (Int)")
    require(minAccum.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMin; case _ => false }}, "Min specialization (Int)")
    require(fma32Accum.exists{reg => reg.writers.exists{case Op(w: RegAccumFMA[_]) => true; case _ => false }}, "FMA32 specialization (Int)")
    require(fma8Accum.exists{reg => reg.writers.exists{case Op(w: RegAccumFMA[_]) => true; case _ => false }}, "FMA8 specialization (Int)")

    val sumAccum_Pipe = regs.filter(_.name.exists(_.startsWith("sumAccum_Pipe")))
    val prodAccum_Pipe = regs.filter(_.name.exists(_.startsWith("prodAccum_Pipe")))
    val maxAccum_Pipe = regs.filter(_.name.exists(_.startsWith("maxAccum_Pipe")))
    val minAccum_Pipe = regs.filter(_.name.exists(_.startsWith("minAccum_Pipe")))
    val fma32Accum_Pipe = regs.filter(_.name.exists(_.startsWith("fma32Accum_Pipe")))
    val fma8Accum_Pipe  = regs.filter(_.name.exists(_.startsWith("fma8Accum_Pipe")))
    require(sumAccum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumAdd; case _ => false }}, "Sum specialization (Int) [Pipe]")
    require(prodAccum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMul; case _ => false }}, "Product specialization (Int) [Pipe]")
    require(maxAccum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMax; case _ => false }}, "Max specialization (Int) [Pipe]")
    require(minAccum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumOp[_]) => w.op == AccumMin; case _ => false }}, "Min specialization (Int) [Pipe]")
    require(fma32Accum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumFMA[_]) => true; case _ => false }}, "FMA32 specialization (Int) [Pipe]")
    require(fma8Accum_Pipe.exists{reg => reg.writers.exists{case Op(w: RegAccumFMA[_]) => true; case _ => false }}, "FMA8 specialization (Int) [Pipe]")

    super.checkIR(block)
  }
}
slac/xppc00117_r136_refsub_ipm4_del3.volume
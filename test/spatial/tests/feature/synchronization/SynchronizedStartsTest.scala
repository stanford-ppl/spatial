package spatial.tests.feature.synchronization

object SSTHelper{
  def contains(a: Option[String], b: String): Boolean = {a.getOrElse("").indexOf(b) != -1}
}


import spatial.dsl._
import argon.Block
import argon.Op
import spatial.node._


@spatial class SynchronizedStartsTest extends SpatialTest {
  // sram2 and sram4 should have P2 duplicates.  All else should have 1
  def main(args: Array[String]): Unit = {
    val dummy = ArgIn[Int]
    setArg(dummy, 5)
    val P1 = 2
    val P2 = 2

    Accel {
      def fillSRAM(s: SRAM1[Int]): Unit = Foreach(s.size by 1){i => s(i) = i}

      // All controllers are synchronized and happy
      val sram1 = SRAM[Int](16*16)
      fillSRAM(sram1)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram1(a0*16 + c0)}")}
        }
      }

      // LAYERC_STAGE0 causes c0 to be non-synchronized (even if STAGE0 and STAGE1 were swapped, since LAYERB is a loop)
      val sram2 = SRAM[Int](16*16)  // Must duplicate for each lane of LAYERA (P1 duplicates)
      fillSRAM(sram2)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(start until 4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram2(a0*16 + c0)}")}
        }
      }

      // All controllers are synchronized.  Even though LAYERB has uid-variant runtime, LAYERA unrolls it as MoP and variance is in outermost iter
      val sram3 = SRAM[Int](16*16)
      fillSRAM(sram3)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 + start by 1, 4 by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram3(a0*16 + c0)}")}
        }
      }

      // No controllers are synchronized, because LAYERA unrolls as PoM and LAYERB causes uid-variant runtime
      val sram4 = SRAM[Int](16*16) // Must duplicate for each lane of LAYERA and LAYERC_STAGE1 (P1*P2 duplicates)
      fillSRAM(sram4)
      'LAYERA.Pipe.POM.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERB.Foreach(4 by 1, 4 + start by 1){(b0, b1) => 
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram4(a0*16 + c0)}")}
        }
      }

      // All controllers are synchronized, but LAYERC_STAGE1 iters of uid(a0)=1 are offset by 1 (mop unrolling of LAYERA)
      val sram5 = SRAM[Int](16*16)
      fillSRAM(sram5)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
        'LAYERC_STAGE1.Foreach(start until 16 by 1 par P2){c0 => println(r"${sram5(a0*16 + c0)}")}
      }

      // No controllers are synchronized, because of unknow Fork injected into the hierarchy
      val sram6 = SRAM[Int](16*16)
      fillSRAM(sram6) // Must duplicate for each lane of LAYERA (P1 duplicates), but LAYERC_STAGE1 is synchronized for each unrolled body
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        if (a0 + dummy.value == 3) {
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram6(a0*16 + c0)}")}
        }
      }

      // All controllers are synchronized, even though there is a branch, because it is forked-iter invariant
      val sram7 = SRAM[Int](16*16)
      fillSRAM(sram7)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        if (dummy.value == 3) {
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram7(a0*16 + c0)}")}
        }
      }

      // No controllers are synchronized, because of unknow Fork injected into the hierarchy
      val sram8 = SRAM[Int](16*16)
      fillSRAM(sram8) // Must duplicate for each lane of LAYERA (P1 duplicates), but LAYERC_STAGE1 is synchronized for each unrolled body
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        Pipe{
          if (a0 + dummy.value == 3) println(r"$dummy")
          'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
          'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => println(r"${sram8(a0*16 + c0)}")}
        }
      }

      // No controllers are synchronized, because of unknow Fork injected into the hierarchy
      val sram9 = SRAM[Int](16*16)
      fillSRAM(sram9) // Must duplicate for each lane of LAYERA and LAYERC_STAGE1 (P1*P2 duplicates)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
        'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => 
          Pipe{println(r"$dummy")}
          Pipe{                                                // <-- c0_0 and c0_1 are synchronized at start of this Pipe.... 
            if (a0 + c0 + dummy.value == 3) println(r"$dummy") // <-- but this lane-dependent runtime causes...
            Pipe{println(r"${sram9(a0*16 + c0)}")}             // <-- them to dephase by the time we get to this Pipe!
          }
        }
      }

      // Controllers branching from LAYERC_STAGE1 are unsynchronized, because of unknow Fork injected into the hierarchy
      val sram10 = SRAM[Int](16*16)
      fillSRAM(sram10) // Must duplicate for each lane of LAYERC_STAGE1 (P2 duplicates)
      'LAYERA.Foreach(16 by 1 par P1, 4 by 1){(a0, a1) => 
        val start = a0 % P1
        'LAYERC_STAGE0.Foreach(4 by 1){_ => println(r"$dummy")}
        'LAYERC_STAGE1.Foreach(16 by 1 par P2){c0 => 
          Pipe{println(r"$dummy")}
          Pipe{                                                 // <-- c0_0 and c0_1 are synchronized at start of this Pipe.... 
            if (c0 + dummy.value == 3) println(r"$dummy")       // <-- but this lane-dependent runtime causes...
            Pipe{println(r"${sram10(a0*16 + c0)}")}             // <-- them to dephase by the time we get to this Pipe!
          }
        }
      }

    }


    assert(true)
  }


  override def checkIR(block: Block[_]): Result = {
    val sram1_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram1_")  => sram }.size
    val sram2_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram2_")  => sram }.size
    val sram3_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram3_")  => sram }.size
    val sram4_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram4_")  => sram }.size
    val sram5_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram5_")  => sram }.size
    val sram6_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram6_")  => sram }.size
    val sram7_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram7_")  => sram }.size
    val sram8_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram8_")  => sram }.size
    val sram9_count = block.nestedStms.collect{case  x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram9_")  => sram }.size
    val sram10_count = block.nestedStms.collect{case x@Op(sram:SRAMNew[_,_]) if SSTHelper.contains(x.name, "sram10_") => sram }.size

    require(sram1_count ==  1, "Should only have 1 duplicate of sram1")
    require(sram2_count ==  2, "Should only have 2 duplicates of sram2")
    require(sram3_count ==  1, "Should only have 1 duplicate of sram3")
    require(sram4_count ==  2, "Should only have 2 duplicates of sram4")
    require(sram5_count ==  1, "Should only have 1 duplicate of sram5")
    require(sram6_count ==  2, "Should only have 2 duplicates of sram6")
    require(sram7_count ==  1, "Should only have 1 duplicate of sram7")
    require(sram8_count ==  2, "Should only have 2 duplicates of sram8")
    require(sram9_count ==  4, "Should only have 4 duplicates of sram9")
    require(sram10_count == 2, "Should only have 2 duplicates of sram10")

    super.checkIR(block)
  }

}

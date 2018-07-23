package spatial.tests.compiler

import argon._

import spatial.dsl._
import spatial.node.DelayLine
import spatial.node.UnrolledForeach
import spatial.metadata.control._

@spatial class FlatAffineAccess extends SpatialTest {

  override def runtimeArgs: Args = "256 16"

  def main(args: Array[String]): Unit = {
    
    val debug:scala.Boolean = false

    val x = args(0).to[Int]
    val y = args(1).to[Int]
    assert(x == 256 && y == 16)

    val size2 = ArgIn[Int]
    val oc = ArgIn[Int]

    setArg(size2, x)
    setArg(oc, y)

    val dram_1D = DRAM[Int](4096)
    val dram_2D = DRAM[Int](16,256)
    val dram_3D = DRAM[Int](16,16,16)
    val dram_rand = DRAM[Int](16)

    Accel {
      val result_1D = SRAM[Int](4096)
      val result_2D = SRAM[Int](16,256)
      val result_3D = SRAM[Int](16,16,16)
      val result_rand = SRAM[Int](16)
      'LOOP1D.Foreach(0 until 16, 0 until 16 par 4, 0 until 16){(a,b,c) => 
        result_1D(c * size2 + a * oc + b) = a + b + c
      }
      'LOOP2D.Foreach(0 until 16, 0 until 16 par 4, 0 until 16){(a,b,c) => 
        result_2D(c, a * oc + b) = a + b + c
      }
      'LOOP3D.Foreach(0 until 16, 0 until 16, 0 until 16 par 4){(a,b,c) => 
        result_3D(c, a, b) = a + b + c
      }
      'LOOPRAND.Foreach(0 until 16 par 4){i =>
        result_rand(result_1D(i)) = i
      }
      dram_1D store result_1D
      dram_2D store result_2D
      dram_3D store result_3D
      dram_rand store result_rand
    }

    val got_1D = getMem(dram_1D)
    val got_2D = getMatrix(dram_2D)
    val got_3D = getTensor3(dram_3D)
    val got_rand = getMem(dram_rand)

    val gold_2D = (0::16,0::256){(i,j) => i+(j/16).to[Int]+(j%16)}
    val gold_3D = (0::16,0::16,0::16){(i,j,k) => i+j+k}
    printArray(got_1D, "Got 1D:")
    printArray(gold_3D.flatten, "Gold 1D:")
    printMatrix(got_2D, "Got 2D:")
    printMatrix(gold_2D, "Gold 2D:")
    printTensor3(got_3D, "Got 3D:")
    printTensor3(gold_3D, "Gold 3D:")
    println(r"DCE-Protection on rand: ${got_rand(0)}")

    val cksum1 = got_1D == gold_3D.flatten 
    val cksum2 = got_2D == gold_2D 
    val cksum3 = got_3D == gold_3D
    val cksum = cksum1 & cksum2 & cksum3
    println(r"1D: $cksum1, 2D: $cksum2, 3D: $cksum3")
    println(r"PASS: ${cksum} (FlatAffineAccess)")
    assert(cksum)
  }

  override def checkIR(block: Block[_]): Result = {
    val iis = block.nestedStms.collect{case x@Op(_:UnrolledForeach) if ((x.name == "LOOP1D") || (x.name == "LOOP2D") || (x.name == "LOOP3D")) => x.II }
    iis.foreach{x => assert(x == 1.0)}
    super.checkIR(block)
  }

}


@spatial class MemReduceII extends SpatialTest {

  override def runtimeArgs: Args = "2 8"

  def main(args: Array[String]): Unit = {
    
    val debug:scala.Boolean = false

    val x = args(0).to[Int]
    val P1 = 2
    val i = args(1).to[Int]

    val X = ArgIn[Int]
    val iters = ArgIn[Int]

    setArg(X, x)
    setArg(iters, i)

    val dram = DRAM[Int](256)

    Accel {
    	val result = SRAM[Int](256)
    	MemReduce(result(0::256 par P1))(iters by 1){i => 
    		val local = SRAM[Int](256)
    		Foreach(256 by 1){j => local(j) = j*X}
    		local
    	}{_+_}
    	dram store result
    }

    val got = getMem(dram)
    val gold = Array.tabulate(256){j => j*i*x}

    printArray(got, "Got:")
    printArray(gold, "Gold:")

    val cksum = got == gold
    println(r"PASS: ${cksum} (FlatAffineAccess)")
    assert(cksum)
  }

  override def checkIR(block: Block[_]): Result = {
    val iis = block.nestedStms.collect{case x@Op(_:UnrolledForeach) => x.II }
    iis.foreach{x => assert(x == 1.0)}
    super.checkIR(block)
  }

}

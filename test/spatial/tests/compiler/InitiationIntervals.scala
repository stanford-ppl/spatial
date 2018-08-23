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


@spatial class IterationDiffs extends SpatialTest {

  override def runtimeArgs: Args = "1"

  def main(args: Array[String]): Unit = {
    
    val debug:scala.Boolean = false

    val x = args(0).to[Int]

    val X = ArgIn[Int]

    val num_tests = 11
    val sum1 = ArgOut[Int]
    val sum2 = ArgOut[Int]
    val tests_run = ArgOut[Int]

    setArg(X, x)

    val dram = DRAM[Int](num_tests,16)

    Accel {
      val track = Reg[Int](0)

      sum1 := 'IIFMA.Reduce(Reg[Int])(16 by 1){i => 
        i*i
      }{_+_}
      sum2 := 'IIADD.Reduce(Reg[Int])(16 by 1){i => 
        i
      }{_+_}
      
      def IIChecker(start: scala.Int, stop: scala.Int, step: scala.Int, P: scala.Int, inits: List[scala.Int], rhsoffset: scala.Int, numoffset: scala.Int, dstrow: scala.Int): Unit = {
        val sram = SRAM[Int](16)
        inits.zipWithIndex.foreach{case (init, i) => if (step > 0) sram(i) = init else sram(15 - i) = init}
        'FREACH.Foreach(start until stop by step par P){ i =>
          sram(i) = sram(i + rhsoffset) * X + X * numoffset
        }
        dram(dstrow.to[Int], 0::16) store sram
        track := track.value ^ (1 << dstrow)
      }

      // 0 2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 
      'TEST0.Pipe{ // II = 1
        val sram0 = SRAM[Int](16)
        MemReduce(sram0(0::16 par 2))(2 by 1){i => 
          val local = SRAM[Int](16)
          Foreach(16 by 1){j => local(j) = j*X}
          local
        }{_+_}
        dram(0.to[Int], 0::16) store sram0
        track := track.value ^ (1 << 0)
      }

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST1.Pipe{IIChecker(1,  16,  1, 1, List(0),         -1, 1, 1)} // II = body latency

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST2.Pipe{IIChecker(2,  16,  1, 1, List(0,1),       -2, 2, 2)} // II = body latency / 2

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST3.Pipe{IIChecker(3,  16,  1, 1, List(0,1,2),     -3, 3, 3)} // II = body latency / 3

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST4.Pipe{IIChecker(1,  16,  1, 2, List(0),         -1, 1, 4)} // II = single lane latency * 2

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST5.Pipe{IIChecker(2,  16,  1, 2, List(0,1),       -2, 2, 5)} // II = single lane latency * 1

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST6.Pipe{IIChecker(2,  16,  1, 3, List(0,1),       -2, 2, 6)} // II = single lane latency * 2

      // 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0
      'TEST7.Pipe{IIChecker(0,  15,  1, 1, List.fill(16)(0),  1, 1, 7)} // II = 1

      // 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1 0
      'TEST8.Pipe{IIChecker(14, -1, -1, 1, List(0),         1, 1, 8)} // II = body latency

      // 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
      'TEST9.Pipe{IIChecker(0, 16, 1, 1, List.tabulate(16){i => i-1}, 0, 1, 9)} // II = 1

      'TEST10.Pipe{  // II = body latency
        val sram11 = SRAM[Int](16)
        Foreach(16 by 1){i => sram11(i) = 0}
        Foreach(48 by 1){i => 
          val addr = Reg[Int](0)
          Foreach(3 by 1){_ => addr := i/3}
          Pipe{sram11(addr.value) = sram11(addr.value) + 1}
        }
        dram(10.to[Int], 0::16) store sram11
        track := track.value ^ (1 << 10)
      }
      tests_run := track
    }

    val got = getMatrix(dram)
    val gold = (0::num_tests, 0::16){(i,j) => 
      if (i == 0) j*2
      else if (i == 7) {if (j < 15) 1 else 0}
      else if (i == 8) 15-j
      else if (i == 10) 3
      else j
    }

    val gold1 = Array.tabulate(16){j => j*j}.reduce{_+_}
    val gold2 = Array.tabulate(16){j => j}.reduce{_+_}

    printMatrix(got, "Got:")
    printMatrix(gold, "Gold:")
    val tests_valid = getArg(tests_run)
    val tests_results = (0 to num_tests-1).map{i => 
      val gotslice = Array.tabulate(16){j => got(i,j)}
      val goldslice = Array.tabulate(16){j => gold(i,j)}
      println("======================")
      if ((tests_valid | (1 << i)) == tests_valid) {
        println(r"Test $i: ")
        printArray(gotslice, "Got:")
        printArray(goldslice, "Gold:")
        println(r"   Matches?  ${gotslice == goldslice}")
        gotslice == goldslice
      } else {
        println(r"Test $i Not Executed")
        true
      }
    }

    (0 to num_tests-1).foreach{i => 
      if ((tests_valid | (1 << i)) == tests_valid) {
        println(r"Test $i: ${tests_results(i)}")
      } else {
        println(r"Test $i: Not Executed")
      }
    }


    println(r"Scalar Reductions: ${getArg(sum1)} =?= $gold1 && ${getArg(sum2)} == $gold2")

    val cksum = tests_results.reduce{_&&_} && getArg(sum1) == gold1 && getArg(sum2) == gold2
    println(r"PASS: ${cksum} (IterationDiffs)")
    assert(cksum)
  }

  // override def checkIR(block: Block[_]): Result = {
  //   val ii0 = block.nestedStms.collect{case x@Op(_:UnitPipe) if x.name == "TEST0" => x }
  //   super.checkIR(block)
  // }

}

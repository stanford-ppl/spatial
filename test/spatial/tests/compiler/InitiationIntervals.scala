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
      val result_rand = SRAM[Int](16).conflictable
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
    //val cksum = cksum1
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

    val num_tests = 13
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

      def IIChecker2(start: scala.Int, stop: scala.Int, step: scala.Int, P: scala.Int, inits: List[I32], rhsoffset: scala.Int, numoffset: scala.Int, dstrow: scala.Int): Unit = {
        // Merge sort variants ripped from DigitRecognition in Rosetta
        val sram = RegFile[Int](16, inits.toSeq)
        'FREACH2.Foreach(start until stop by step par P){ i => 
          val a = sram(i)
          val b = sram(i+1)
          val lower = mux(a < b, a, b)
          val upper = mux(a < b, b, a)
          sram(i) = lower
          sram(i+1) = upper
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

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = body latency
       *   O   O   O   O      
       *   |___^   ---> 
       */
      'TEST1.Pipe{IIChecker(1,  16,  1, 1, List(0),         -1, 1, 1)}

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = body latency / 2
       *   O   O   O   O      
       *   |_______^   --->
       */
      'TEST2.Pipe{IIChecker(2,  16,  1, 1, List(0,1),       -2, 2, 2)}

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = body latency / 3
       *   O   O   O   O      
       *   |___________^  --->
       */
      'TEST3.Pipe{IIChecker(3,  16,  1, 1, List(0,1,2),     -3, 3, 3)}

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = single lane latency * 2, has double segmentation
       *   O   O   O   O      
       *   |___^|__^    --->
       */
      'TEST4.Pipe{IIChecker(1,  16,  1, 2, List(0),         -1, 1, 4)}

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = single lane latency * 1
       *   O   O   O   O        
       *   |___|___^   ^  --->
       *       |_______|
       */
      'TEST5.Pipe{IIChecker(2,  16,  1, 2, List(0,1),       -2, 2, 5)}

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = single lane latency * 2, has triple segmentation
       *   O   O   O   O      
       *   |___^|__^|__^ --->
       */
      'TEST6.Pipe{IIChecker(2,  16,  1, 3, List(0,1),       -2, 2, 6)} 

      /* 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0  // II = 1
       *   O   O   O   O 
       *   ^___| --->
       */
      'TEST7.Pipe{IIChecker(0,  15,  1, 1, List.fill(16)(0),  1, 1, 7)} 

      /* 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1 0  // II = body latency
       *  O   O   O   O      
       *     <--- ^___|
       */
      'TEST8.Pipe{IIChecker(14, -1, -1, 1, List(0),         1, 1, 8)} 

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = 1
       *   O   O   O   O      
       *  |_^   --->
       */
      'TEST9.Pipe{IIChecker(0, 16, 1, 1, List.tabulate(16){i => i-1}, 0, 1, 9)} 

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

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 20  // II = single lane latency * 1, has double segmentation
       *   O   O   O   O      
       *   |_X_|   |  --->
       *       |_X_|
       */
      'TEST11.Pipe{IIChecker2(0, 15, 1, 2, List(20,0,1,2,3,4,5,6,7,8,9,10,11,12,13,14), 0, 0, 11)} 

      /* 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15  // II = 1
       *   O   O   O   O    
       *   |_X_|   |_X_|  --->
       */
      'TEST12.Pipe{IIChecker2(0, 16, 2, 2, List(1,0,3,2,5,4,7,6,9,8,11,10,13,12,15,14), 0, 0, 12)} 

      tests_run := track
    }

    val got = getMatrix(dram)
    val gold = (0::num_tests, 0::16){(i,j) => 
      if (i == 0) j*2
      else if (i == 7) {if (j < 15) 1 else 0}
      else if (i == 8) 15-j
      else if (i == 10) 3
      else if (i == 11 && j == 15) 20
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

@spatial class LoopInvariantCycle extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  def main(args: Array[String]): Unit = {
    type T = Int
    val out = DRAM[Int](3,3,8)
    val A = ArgIn[Int]
    val B = ArgIn[Int]
    val C = ArgIn[Int]
    val D = ArgIn[Int]
    val E = ArgIn[Int]
    val ONE = ArgIn[Int]
    setArg(A, 16)
    setArg(B, 16)
    setArg(C, 9)
    setArg(D, 3)
    setArg(E, 8)
    setArg(ONE, 1)
    val m = ArgOut[Int]
    Accel {
      val x = SRAM[T](8, 16).hierarchical.noduplicate
      Foreach(8 by 1, A by 1 par 4){(i,j) => x(i,j) = (i + j).to[T]}

      val y = SRAM[T](3, 3, 8).buffer
      Foreach(A by B par 1) { i =>
        val z = SRAM[T](3136)
        Foreach(B*C by 1 par 4){i => z(i) = i.to[T]}

        // If you want II of 1, you have to reorder the loops so that ib is the outermost (or just outermore) iterator
        Foreach(0::D par 1, 0::D par 1 /*7*/, 0::B par 1, 0::E par 2) { case List(r,c,ib,b) =>  // Should choose iterDiff = Some(1) (maximally conservative)
          val loaded_val = z(ib.to[Int]*C + r*D + c)
          val partial_sum = loaded_val * x(b, i.to[Int] + ib.to[Int])
          println(r"ib $ib, i $i: y($r,$c,$b) = z($ib*$C + $r*$D + $c) * x($b, $i, $ib) ($loaded_val * ${x(b, i.to[Int] + ib.to[Int])})")
          y(r,c,b) = mux(ib == 0  &&  i == 0, partial_sum, y(r,c,b) + partial_sum)
        }
      }
      out store y

      // Dumb example of loop invariant accumulation cycle
      val sram = SRAM[Int](8,2)
      Foreach(8 by 1, 2 by 1){(i,j) => sram(i,j) = 0}
      Foreach(5 by 1, 2 by 1){(i,j) => sram(0,j) = sram(0,j) + i * ONE.value * ONE.value}
      m := sram(0,0)
    }


    printTensor3(getTensor3(out), "Got:")
    val gold = Array[Int](11160,12240,13320,14400,15480,16560,17640,18720,
                          11280,12376,13472,14568,15664,16760,17856,18952,
                          11400,12512,13624,14736,15848,16960,18072,19184,
                          11520,12648,13776,14904,16032,17160,18288,19416,
                          11640,12784,13928,15072,16216,17360,18504,19648,
                          11760,12920,14080,15240,16400,17560,18720,19880,
                          11880,13056,14232,15408,16584,17760,18936,20112,
                          12000,13192,14384,15576,16768,17960,19152,20344,
                          12120,13328,14536,15744,16952,18160,19368,20576,
                         ).reshape(3,3,8) // Too lazy to derive the answer... :(
    printTensor3(gold, "Gold:")
    println(r"Dumb test got ${getArg(m)}, wanted ${0 + 1 + 2 + 3 + 4}")
    assert(getTensor3(out) == gold)
    assert(getArg(m) == 0 + 1 + 2 + 3 + 4)
  }

}


@spatial class MultiTriplet extends SpatialTest {
  // App that contains multiple accumulator triplets 

  override def runtimeArgs: Args = "2"

  def main(args: Array[String]): Unit = {

    val x = ArgIn[Int]
    val in = args(0).to[Int]
    assert(in == 2, "Must make input arg == 2")
    setArg(x, in)
    val y = ArgOut[Int]

    Accel {
      val r1 = Reg[Int](0)
      val r2 = Reg[Int](0)
      val first = Reg[Bit](false)
      Foreach(5 by 1){i =>
        val v = i
        val t = x.value
        if (v > 0) {
          val t = r1.value
          r2 := t * 2 + x.value - 1
          r1 := t * 2
        } else {
          r1 := (x.value + 3) / 5
        }
      }
      y := r2.value
    }

    val result = getArg(y)
    val gold = 17
    println(r"Got $result, wanted $gold")
    assert(result == gold)
  }

}

@spatial class Blur extends SpatialTest { 
  // A tricky case for iteration diff analyzer
  def main(args: Array[String]): Void = {
    val M = 8
    val N = 8

    val iters = ArgIn[Int]
    setArg(iters, 5)
    val out = DRAM[Int](1,N)

    Accel {
      val sram = SRAM[Int](M,N).noduplicate.buffer
      Foreach(iters.value by 1) { _ => 
        Foreach(N by 1){j => sram(0,j) = j}
        Foreach(1 until M by 1, N by 1 par N/2){(i,j) =>  // II = 1 for par = 1 and then increases for higher pars
          val left = Reg[Int]
          val mid = Reg[Int]
          val right = Reg[Int]
          left := mux(j == 0, 0, sram(i-1, j-1))
          mid := sram(i-1, j)
          right := mux(j == N-1, 0, sram(i-1, j+1))
          sram(i,j) = left.value + mid.value + right.value

          // val left = mux(j == 0, 0, sram(i-1, j-1))
          // val mid = sram(i-1, j)
          // val right = mux(j == N-1, 0, sram(i-1, j+1))
          // sram(i,j) = left + mid + right
        }
        out(0::1, 0::N) store sram(M-1 :: M, 0::N)
      }
    }

    val numNeighbors = 2*(M-1) + 1
    // Too lazy to derive the answer :(
    val gold = Array[Int](1437,2993,4689,6358,7600,7848,6611,3813)
    val result = getMatrix(out).flatten
    printArray(result, "Got")
    printArray(gold, "Gold")
    assert(result == gold)
  }
}  

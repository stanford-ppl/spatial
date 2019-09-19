package spatial.tests.feature.banking

import spatial.dsl._
import argon.Block

@spatial class Bank4DHard extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  // Not really "hard", but used to keep ExhaustiveBanking churning for a crazy long time
  val I = 16; val R = 3; val C = 3; val O = 16
  val PI = 2; val PR = 3;val PC= 3; val PO = 2

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](I,R,C,O)
    val data = (0::I,0::R,0::C,0::O){(i,j,k,l) => (i+j+k+l) % 5}
    setMem(dram,data)
    val out = ArgOut[Int]
    Accel {
      val x_hier = SRAM[Int](I,R,C,O).nofission.hierarchical // flat is also possible but takes way longer to find
      x_hier load dram

      out := Reduce(Reg[Int])(0 until I par PI, 0 until R par PR, 0 until C par PC, 0 until O par PO){case List(i,r,c,o) =>
        x_hier(i,r,c,o)
      }{_+_}
    }

    val gold = data.reduce{_+_}
    val got = getArg(out)
    println(r"Got $got, expected $gold")
    println(r"PASS: ${got == gold}")
    assert(got == gold)
  }
}

@spatial class Bankapalooza extends SpatialTest {
  // This app shows how to use all kinds of banking directives
  val W0 = 1
  val W1 = 2
  val W2 = 2
  val R0 = 3
  val R1 = 2
  val R2 = 4
  val WINDOWR0 = 2
  val WINDOWR1 = 3
  val WINDOWR2 = 3

  def main(args: Array[String]): Unit = {
    val in = ArgIn[Int]
    setArg(in, 1.to[Int])
    val out = ArgOut[Int]
    val outs = List.tabulate(11){i => ArgOut[Int]}
    Accel {
      def fill(x: SRAM3[Int]): Unit = { // hierarchical parallel write of the sram (must bank for all lanes)
        Foreach(x.dim0 by 1 par W0, x.dim1 by 1 par W1, x.dim2 by 1 par W2){(i,j,k) => x(i,j,k) = i + j + k}
      }
      def read(x: SRAM3[Int]): Reg[Int] = {  // hierarchical parallel read of the sram
        val reg = Reg[Int]
        Pipe{
          val dst = SRAM[Int](x.dim0, x.dim1, x.dim2).hierarchical.effort(0) // Don't care how it banks this
          Foreach(x.dim0 par R0, x.dim1 par R1, x.dim2 par R2) {(i,j,k) => dst(i,j,k) = x(i,j,k)}
          reg := dst(in.value,in.value,in.value)
        }
        reg
      }
      def slidingWindowRead(x: SRAM3[Int]): Reg[Int] = { // 3x3 sliding window on each page of the sram
        val reg = Reg[Int]
        Pipe{
          val dst = SRAM[Int](x.dim0, x.dim1, x.dim2).hierarchical.effort(0) // Don't care how it banks this
          Foreach(x.dim0 by 1 par WINDOWR0, x.dim1 - {WINDOWR1-1} by 1, x.dim2 - {WINDOWR2-1} by 1) {(i,j,k) => 
            dst(i,j,k) = List.tabulate(WINDOWR1,WINDOWR2){(jj,kk) => x(i,j+jj,k+kk)}.flatten.reduceTree{_+_}
          }
          reg := dst(in.value,in.value,in.value)
        }
        reg

      }
      def run(x: SRAM3[Int]): Reg[Int] = {
        fill(x)
        read(x)
      }
      def slidingWindowRun(x: SRAM3[Int]): Reg[Int] = {
        fill(x)
        slidingWindowRead(x)
      }

      // Hierarchical scheme, 1 duplicate
      val hier = SRAM[Int](3,12,8).hierarchical.nofission
      val r1 = run(hier)

      // Flat scheme, 1 duplicate
      val flat = SRAM[Int](3,12,8).flat.nofission
      val r2 = run(flat)

      // Could choose anything, should take relatively long to bank
      val any = SRAM[Int](3,12,8).effort(2)                  
      val r3 = run(any)

      // Hierarchical scheme, 24 duplicates
      val hierdup = SRAM[Int](3,12,8).hierarchical.fullfission
      val r4 = run(hierdup)

      // Hierarchical scheme, either 3 duplicates (first option) or 8 duplicates (second option)
      val hiersomedup = SRAM[Int](3,12,8).hierarchical.axesfission(List( List(0), List(1,2) ) )
      val r5 = run(hiersomedup)

      // Flat scheme, 1 duplicate (should have really expensive histogram)
      val windowflat = SRAM[Int](3,12,8).flat.nofission
      val r6 = slidingWindowRun(windowflat)

      // Hierarchical scheme, 1 duplicate (should have really expensive histogram)
      val windowhier = SRAM[Int](3,12,8).hierarchical.nofission
      val r7 = slidingWindowRun(windowhier)

      // Any scheme, 2 duplicates (should have really expensive histogram)
      val windowdup = SRAM[Int](3,12,8).axesfission(List(List(0)))
      val r8 = slidingWindowRun(windowdup)

      // "Weird" case where you have a regular access pattern and a static column in parallel
      val noblockcyc = SRAM[Int](2,32).hierarchical.noblockcyclic.nofission // Needs 32 banks
      val blockcyc = SRAM[Int](2,32).hierarchical.onlyblockcyclic.nofission // Needs fewer banks (funky scheme) but more darkVolume
      Foreach(2 by 1, 32 by 1){(i,j) => noblockcyc(i,j) = i + j; blockcyc(i,j) = i + j}
      val r9 = Reduce(Reg[Int])(2 by 1, 31 by 1){(i,j) => noblockcyc(i,j) + noblockcyc(i,31)}{_+_}
      val r10 = Reduce(Reg[Int])(2 by 1, 31 by 1){(i,j) => blockcyc(i,j) + blockcyc(i,31)}{_+_}


      out := r1.value + r2.value + r3.value + r4.value + r5.value + r6.value + r7.value + r8.value + r9.value + r10.value

      outs(1) := r1.value
      outs(2) := r2.value
      outs(3) := r3.value
      outs(4) := r4.value
      outs(5) := r5.value
      outs(6) := r6.value
      outs(7) := r7.value
      outs(8) := r8.value
      outs(9) := r9.value
      outs(10) := r10.value
    }

    val gold = 3 * 5 /*run*/ + 45 * 3 /*slidingWindow*/ + 2914 * 2 /*block cyclics*/
    val got = getArg(out)
    println(r"Got $got, expected $gold")
    outs.foreach{println(_)}
    println(r"PASS: ${got == gold}")
    assert(got == gold)
  }
}


@spatial class BroadcastRandom extends SpatialTest {
  override def compileArgs: Args = super.compileArgs and "--forceBanking"

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](64)
    val data = Array.tabulate(64){i => i % 5}
    setMem(dram,data)
    val out = DRAM[Int](3,3,64)
    Accel {
      val x = SRAM[Int](64).nofission // Assert there is only 2 copies of this (or 1 copy if k is par 1)
      x load dram
      val y = SRAM[Int](3,3,64).nofission
      val z = SRAM[Int](3,3).nofission
      Foreach(3 by 1, 3 by 1){(i,j) => z(i,j) = i+j}

      Foreach(10 by 1){i => 
        Foreach(3 by 1, 3 by 1 par 2 /*1*/, 64 by 1 par 16){ (j,k,l) => 
          val cond1 = i % 2 == 0
          val cond2 = i % 3 == 0
          y(j,k,l) = x(mux(cond1, j, j*2) + mux(cond2, k, k*2)) + z(j,k)
        }
      }

      out store y
    }

    printTensor3(getTensor3(out), "Got:")
    val gold = (0::3, 0::3, 0::64){(j,k,l) => 
      val a = j*2
      val b = k
      data(a + b) + (j + k)
    }
    printTensor3(gold, "Gold:")
    assert(getTensor3(out) == gold)
  }

  override def checkIR(block: Block[_]): Result = {
    // the IR should have only 3 SRAMNew nodes
    super.checkIR(block)
  }


}

@spatial class BankLines extends SpatialTest {

  def main(args: Array[String]): Unit = {

    val P1 = 1
    val P2 = 3
    val P3 = 4
    val loadPar = 1

    // Memories
    val INPUT_DATA = DRAM[Int](5, 5, 32)
    val OUTPUT_DATA = DRAM[Int](5, 5, 32)

    val MAXROWS = ArgIn[Int]
    val MAXCOLS = ArgIn[Int]
    val ARG = ArgIn[Int]
    setArg(ARG, 1)
    setArg(MAXROWS, 5)
    setArg(MAXCOLS, 5)

    // Load data (placeholder)
    val input = (0::5, 0::5, 0::32) {(i,j,k) => ((i + j + k) % 64 - 8).to[Int]}

    // Set data
    setMem(INPUT_DATA, input)

    Accel{
      val in_sram = SRAM[Int](3,3,32).hierarchical.nofission
      Foreach(5 by 1){row =>
        Foreach(5 by 1){col => 
          val idx0 = row * ARG.value
          val idx1 = col * ARG.value
          Foreach(0 until 3 by 1 par P1, 0 until 3 by 1 par P2){(i,j) => 
            if (idx0 - 1 + i >= 0 && idx0 - 1 + i < MAXROWS.value && idx1 - 1 + j >= 0 && idx1 - 1 + j < MAXCOLS.value) in_sram(i::i+1,j::j+1,0::32) load INPUT_DATA(idx0-1+i::idx0+i, idx1-1+j::idx1+j, 0::32 par loadPar)
          }
          val out_sram = SRAM[Int](32)
          Foreach(32 by 1 par P3){k => 
            val data = List.tabulate(3){i => List.tabulate(3){j => 
              if (row - 1 + i >= 0 && row - 1 + i < 5 && col - 1 + j >= 0 && col - 1 + j < 5) in_sram(i,j,k) else 0
            }}.flatten.reduceTree{_+_} 

            out_sram(k) = data
          }
          OUTPUT_DATA(row,col,0::32) store out_sram
        }
      }
    }

    // Get results
    val results = getTensor3(OUTPUT_DATA)
    val gold = (0::5, 0::5, 0::32){(i,j,k) => 
      (0::3, 0::3){(ii,jj) => 
        if (i - 1 + ii >= 0 && i - 1 + ii < 5 && j - 1 + jj >= 0 && j - 1 + jj < 5) input(i-1+ii,j-1+jj,k) else 0
      }.flatten.reduce{_+_}
    }
    printTensor3(results, "Got: ")
    printTensor3(gold, "Wanted: ")
    println(r"PASS: ${gold == results}")
    assert(gold == results)



  }
}

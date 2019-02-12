package spatial.tests.apps


import spatial.dsl._


@spatial class SW_alg extends SpatialTest { // Name SW conflicts with something in spade
  override def runtimeArgs: Args = {
    "tcgacgaaataggatgacagcacgttctcgtattagagggccgcggtacaaaccaaatgctgcggcgtacagggcacggggcgctgttcgggagatcgggggaatcgtggcgtgggtgattcgccggc ttcgagggcgcgtgtcgcggtccatcgacatgcccggtcggtgggacgtgggcgcctgatatagaggaatgcgattggaaggtcggacgggtcggcgagttgggcccggtgaatctgccatggtcgat"
  }

  /*

   Smith-Waterman Genetic Alignment algorithm

   This is just like SW algorithm, except negative scores are capped at 0, backwards traversal starts at highest score from any
      element on the perimeter, and end when score is 0


     [SIC] SW diagram
     LETTER KEY:         Scores                   Ptrs
       a = 0                   T  T  C  G                T  T  C  G
       c = 1                0 -1 -2 -3 -4 ...         0  ←  ←  ←  ← ...
       g = 2             T -1  1  0 -1 -2          T  ↑  ↖  ←  ←  ←
       t = 3             C -2  0 -1  1  0          C  ↑  ↑  ↑  ↖  ←
       - = 4             G -3 -2 -2  0  2          G  ↑  ↑  ↑  ↑  ↖
       _ = 5             A -4 -3 -3 -1  1          A  ↑  ↑  ↑  ↑  ↖
                            .                         .
                            .                         .
                            .                         .

     PTR KEY:
       ← = 0 = skipB
       ↑ = 1 = skipA
       ↖ = 2 = align
  */

  @struct case class sw_tuple(score: Int16, ptr: Int16)
  @struct case class entry_tuple(row: I32, col: I32, score: Int16)


  def main(args: Array[String]): Unit = {

    val a = 'a'.to[Int8]
    val c = 'c'.to[Int8]
    val g = 'g'.to[Int8]
    val t = 't'.to[Int8]
    val d = '-'.to[Int8]
    val dash = ArgIn[Int8]
    setArg(dash,d)
    val underscore = '_'.to[Int8]

    val par_load = 16
    val par_store = 16
    val row_par = 2 (1 -> 1 -> 8)

    val SKIPB = 0
    val SKIPA = 1
    val ALIGN = 2
    val MATCH_SCORE = 2
    val MISMATCH_SCORE = -1
    val GAP_SCORE = -1
    val seqa_string = args(0)
    val seqb_string = args(1)
    val measured_length = seqa_string.length
    val length = ArgIn[Int]
    val lengthx2 = ArgIn[Int]
    setArg(length, measured_length)
    setArg(lengthx2, measured_length*2)
    val max_length = 512
    assert(max_length >= length.value, "Cannot have string longer than 512 elements")

    // TODO: Support c++ types with 2 bits in dram
    val seqa_bin = seqa_string.map{c => c.to[Int8] }
    val seqb_bin = seqb_string.map{c => c.to[Int8] }

    val seqa_dram_raw = DRAM[Int8](length)
    val seqb_dram_raw = DRAM[Int8](length)
    val seqa_dram_aligned = DRAM[Int8](lengthx2)
    val seqb_dram_aligned = DRAM[Int8](lengthx2)
    setMem(seqa_dram_raw, seqa_bin)
    setMem(seqb_dram_raw, seqb_bin)

    Accel{
      val seqa_sram_raw = SRAM[Int8](max_length)
      val seqb_sram_raw = SRAM[Int8](max_length)
      val seqa_fifo_aligned = FIFO[Int8](max_length*2)
      val seqb_fifo_aligned = FIFO[Int8](max_length*2)

      seqa_sram_raw load seqa_dram_raw(0::length par par_load)
      seqb_sram_raw load seqb_dram_raw(0::length par par_load)

      val score_matrix = SRAM[sw_tuple](max_length+1,max_length+1)

      val entry_point = Reg[entry_tuple]
      // Build score matrix
      Reduce(entry_point)(length+1 by 1 par row_par){ r =>
        val possible_entry_point = Reg[entry_tuple]
        val this_body = r % row_par
        Sequential.Foreach(-this_body until length+1 by 1) { c => // Bug #151, should be able to remove previous_result reg when fixed
          val previous_result = Reg[sw_tuple]
          val update = if (r == 0) (sw_tuple(0, 0)) else if (c == 0) (sw_tuple(0, 1)) else {
            val match_score = mux(seqa_sram_raw(c-1) == seqb_sram_raw(r-1), MATCH_SCORE.to[Int16], MISMATCH_SCORE.to[Int16])
            val from_top = score_matrix(r-1, c).score + GAP_SCORE
            val from_left = previous_result.score + GAP_SCORE
            val from_diag = score_matrix(r-1, c-1).score + match_score
            mux(from_left >= from_top && from_left >= from_diag, sw_tuple(from_left, SKIPB), mux(from_top >= from_diag, sw_tuple(from_top,SKIPA), sw_tuple(from_diag, ALIGN)))
          }
          previous_result := update
          if ((c == length.value | r == length.value) && possible_entry_point.score < update.score) possible_entry_point := entry_tuple(r, c, update.score)
          if (c >= 0) {score_matrix(r,c) = sw_tuple(max(0, update.score),update.ptr)}
        }
        possible_entry_point
      }{(a,b) => mux(a.score > b.score, a, b)}

      val traverseState = 0
      val padBothState = 1
      val doneState = 2

      val b_addr = Reg[Int](0)
      val a_addr = Reg[Int](0)
      Parallel{b_addr := entry_point.row; a_addr := entry_point.col}
      val done_backtrack = Reg[Bit](false)
      FSM(0){state => state != doneState }{ state =>
        if (state == traverseState) {
          if (score_matrix(b_addr,a_addr).ptr == ALIGN.to[Int16]) {
            seqa_fifo_aligned.enq(seqa_sram_raw(a_addr-1), !done_backtrack)
            seqb_fifo_aligned.enq(seqb_sram_raw(b_addr-1), !done_backtrack)
            done_backtrack := b_addr == 1.to[Int] || a_addr == 1.to[Int]
            b_addr :-= 1
            a_addr :-= 1
          } else if (score_matrix(b_addr,a_addr).ptr == SKIPA.to[Int16]) {
            seqb_fifo_aligned.enq(seqb_sram_raw(b_addr-1), !done_backtrack)
            seqa_fifo_aligned.enq(dash, !done_backtrack)
            done_backtrack := b_addr == 1.to[Int]
            b_addr :-= 1
          } else {
            seqa_fifo_aligned.enq(seqa_sram_raw(a_addr-1), !done_backtrack)
            seqb_fifo_aligned.enq(dash, !done_backtrack)
            done_backtrack := a_addr == 1.to[Int]
            a_addr :-= 1
          }
        } else if (state == padBothState) {
          seqa_fifo_aligned.enq(underscore, !seqa_fifo_aligned.isFull) // I think this FSM body either needs to be wrapped in a body or last enq needs to be masked or else we are full before FSM sees full
          seqb_fifo_aligned.enq(underscore, !seqb_fifo_aligned.isFull)
        } else {}
      } { state =>
        mux(state == traverseState && (score_matrix(b_addr,a_addr).score == 0.to[Int16]), doneState, state)
      }

      Parallel{
        seqa_dram_aligned(0::seqa_fifo_aligned.numel par par_store) store seqa_fifo_aligned
        seqb_dram_aligned(0::seqb_fifo_aligned.numel par par_store) store seqb_fifo_aligned
      }

    }

    val seqa_aligned_result = getMem(seqa_dram_aligned)
    val seqb_aligned_result = getMem(seqb_dram_aligned)
    val seqa_aligned_string = charArrayToString(seqa_aligned_result.map(_.to[U8]))
    val seqb_aligned_string = charArrayToString(seqb_aligned_result.map(_.to[U8]))

    // Pass if >75% match
    val matches = seqa_aligned_result.zip(seqb_aligned_result){(a,b) => if ((a == b) || (a == d) || (b == d)) 1 else 0}.reduce{_+_}
    val cksum = matches.to[Float] > 0.75.to[Float]*measured_length.to[Float]*2

    println("Result A: " + seqa_aligned_string)
    println("Result B: " + seqb_aligned_string)
    println("Found " + matches + " matches out of " + measured_length*2 + " elements")
    println("PASS: " + cksum + " (SW)")
    assert(cksum)
  }
}
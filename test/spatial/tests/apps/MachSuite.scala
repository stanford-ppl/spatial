package spatial.tests.apps

import spatial.dsl._
import spatial.targets._

@spatial class AES extends SpatialTest {
  override def runtimeArgs: Args = "50"

  /*
  TODO: Optimize/parallelize many of the memory accesses here and pipeline as much as possible
  
  MachSuite Concerns: 
    - Mix rows math seemed wrong in their implementation
    - Not exactly sure what was going on with their expand_key step
  */
  type UInt8 = FixPt[FALSE,_8,_0]

  def main(args: Array[String]): Unit = {
    // Setup off-chip data
    // val text_in = "the sharkmoster"
    // val plaintext = argon.lang.String.string2num(text_in)
    val plaintext = Array[UInt8](0,17,34,51,68,85,102,119,136,153,170,187,204,221,238,255)
    val key = Array.tabulate(32){i => i.to[UInt8]}
    val sbox = Array[UInt8]( // 256 elements
      0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5,
      0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
      0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0,
      0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
      0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc,
      0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
      0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a,
      0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
      0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0,
      0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
      0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b,
      0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
      0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85,
      0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
      0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5,
      0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
      0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17,
      0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
      0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88,
      0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
      0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c,
      0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
      0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9,
      0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
      0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6,
      0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
      0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e,
      0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
      0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94,
      0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
      0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68,
      0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    )
    
    val par_load = 16
    val par_store = 16
    val outer_par = 1 (1 -> 1 -> 4)

    // Setup
    val num_bytes = ArgIn[Int]
    setArg(num_bytes, 16*args(0).to[Int])

    // Create DRAMs
    val plaintext_dram = DRAM[UInt8](16)
    val key_dram = DRAM[UInt8](32)
    val sbox_dram = DRAM[UInt8](256)
    val ciphertext_dram = DRAM[Int](num_bytes)

    // Transfer data to DRAMs
    setMem(plaintext_dram, plaintext)
    setMem(key_dram, key)
    setMem(sbox_dram, sbox)

    // Debugging support
    val niter = 15
    // val niter = ArgIn[Int]
    // setArg(niter, args(0).to[Int])
    // val key_debug = DRAM[UInt8](32)

    Accel{
      // Setup data structures
      val plaintext_flat = SRAM[UInt8](16).buffer
      val plaintext_sram = RegFile[UInt8](4,4).buffer
      val sbox_sram = SRAM[UInt8](256)
      val key_sram = SRAM[UInt8](32).buffer
      // val mix_lut = LUT[Int](4,4)(
      //    2, 3, 1, 1,
      //    1, 2, 3, 1,
      //    1, 1, 2, 3,
      //    3, 1, 1, 2
      //  )
      val rcon = Reg[UInt8](1).buffer

      // Specify methods
      def expand_key(): Unit = {
        val addr_lut = LUT[Int](4)(29, 30, 31, 28)
        Foreach(4 by 1) { i => 
          key_sram(i) = key_sram(i) ^ sbox_sram(key_sram(addr_lut(i)).to[Int]) ^ mux(i.to[I32] == 0, rcon.value, 0)
        }
        // Pipe{key_sram(0) = key_sram(0) ^ sbox_sram(key_sram(29).as[UInt16].as[Int]) ^ rcon}
        // Pipe{key_sram(1) = key_sram(1) ^ sbox_sram(key_sram(30).as[UInt16].as[Int])}
        // Pipe{key_sram(2) = key_sram(2) ^ sbox_sram(key_sram(31).as[UInt16].as[Int])}
        // Pipe{key_sram(3) = key_sram(3) ^ sbox_sram(key_sram(28).as[UInt16].as[Int])}
        rcon := (((rcon)<<1) ^ ((((rcon)>>7) & 1) * 0x1b))

        Sequential.Foreach(4 until 16 by 4) {i =>
          Sequential.Foreach(4 by 1) {j => 
            key_sram(i.to[I32]+j.to[I32]) = key_sram(i.to[I32]+j.to[I32]) ^ key_sram(i.to[I32] - 4 + j.to[I32])
          }
          // Pipe{key_sram(i) = key_sram(i) ^ key_sram(i-4)}
          // Pipe{key_sram(i+1) = key_sram(i+1) ^ key_sram(i-3)}
          // Pipe{key_sram(i+2) = key_sram(i+2) ^ key_sram(i-2)}
          // Pipe{key_sram(i+3) = key_sram(i+3) ^ key_sram(i-1)}
        }
      
        Sequential.Foreach(16 until 20 by 1){i => 
          key_sram(i) = key_sram(i) ^ sbox_sram(key_sram(i.to[I32]-4).to[Int])
        }
        // Pipe{key_sram(16) = key_sram(16) ^ sbox_sram(key_sram(12).as[UInt16].as[Int])}
        // Pipe{key_sram(17) = key_sram(17) ^ sbox_sram(key_sram(13).as[UInt16].as[Int])}
        // Pipe{key_sram(18) = key_sram(18) ^ sbox_sram(key_sram(14).as[UInt16].as[Int])}
        // Pipe{key_sram(19) = key_sram(19) ^ sbox_sram(key_sram(15).as[UInt16].as[Int])}

        Sequential.Foreach(20 until 32 by 4) {i => 
          Sequential.Foreach(4 by 1) { j => 
            key_sram(i.to[I32]+j.to[I32]) = key_sram(i.to[I32]+j.to[I32]) ^ key_sram(i.to[I32] - 4 + j.to[I32])
          }
          // Pipe{key_sram(i) = key_sram(i) ^ key_sram(i-4)}
          // Pipe{key_sram(i+1) = key_sram(i+1) ^ key_sram(i-3)}
          // Pipe{key_sram(i+2) = key_sram(i+2) ^ key_sram(i-2)}
          // Pipe{key_sram(i+3) = key_sram(i+3) ^ key_sram(i-1)}
        }
      }

      def shift_rows(): Unit = {
        Sequential.Foreach(4 by 1){ i => 
          val row = RegFile[UInt8](4) 
          Foreach(4 by 1){ j => 
            val col_addr = (j.to[I32] - i.to[I32]) % 4
            row(col_addr) = plaintext_sram(i,j)
          }
          Foreach(4 by 1){ j => 
            plaintext_sram(i,j) = row(j)
          }
        }
      }

      def substitute_bytes(): Unit = {
        Sequential.Foreach(4 by 1, 4 by 1){(i,j) => 
          val addr = plaintext_sram(i,j).as[UInt16].as[Int] // Upcast without sign-extend
          val subst = sbox_sram(addr)
          plaintext_sram(i,j) = subst
        }
      }

      def rj_xtime(x: UInt8): UInt8 = {
        mux(((x & 0x80.to[UInt8]) > 0.to[UInt8]), ((x << 1) ^ 0x1b.to[UInt8]), x << 1)
      }

      def mix_columns(): Unit = {
        Sequential.Foreach(4 by 1){j => 
          val col = RegFile[UInt8](4)
          Foreach(4 by 1 par 4) { i => col(i) = plaintext_sram(i,j) }
          val e = Reduce(Reg[UInt8](0))(4 by 1 par 4) { i => col(i) }{_^_}
          // val e = col(0) ^ col(1) ^ col(2) ^ col(3)
          Foreach(4 by 1) { i => 
            val id1 = (i.to[I32]+1)%4
            plaintext_sram(i,j) = col(i) ^ e ^ rj_xtime(col(i) ^ col(id1))
          }
        }
      }

      def add_round_key(round: I32): Unit = {
        Foreach(4 by 1, 4 by 1) { (i,j) => 
          val key = mux(round % 2 == 1, key_sram(i.to[I32]+j.to[I32]*4+16), key_sram(i.to[I32]+j.to[I32]*4))
          plaintext_sram(i,j) = plaintext_sram(i,j) ^ key
        }
      }

      // Load structures
      sbox_sram load sbox_dram(0::256 par par_load)

      Foreach(num_bytes by 16 par outer_par){block_id => 
        plaintext_flat load plaintext_dram(0::16 par par_load) // TODO: Allow dram loads to reshape (gh issue #83)
        key_sram load key_dram(0::32 par par_load)
        rcon := 1

        // gh issue #83
        Sequential.Foreach(4 by 1 par 1){i => 
          Sequential.Foreach(4 by 1 par 1){j => 
            plaintext_sram(i,j) = plaintext_flat(j.to[I32]*4+i.to[I32]) // MachSuite flattens columnwise... Why????
          }
        }

        /* Loopy version */
        Sequential.Foreach(niter by 1) { round => 
          // SubBytes
          if (round > 0) {
            Pipe{substitute_bytes()}
          }

          // ShiftRows
          if (round > 0) {
            Pipe{shift_rows()}
          }

          // MixColumns
          if (round > 0 && round < 14 ) {
            Pipe{mix_columns()}
          }

          // Expand key
          if (round > 0 && ((round % 2) == 0)) {
            Pipe{expand_key()}
          }

          // AddRoundKey
          add_round_key(round)

        }

        // /* Partially pipelined version */
        // // Round 0
        // add_round_key(0)

        // // Rounds 1 - 7
        // Sequential.Foreach(1 until 8 by 1) { round => 
        //   substitute_bytes()
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   if ((round % 2) == 0) {
        //     Pipe{expand_key()}
        //   }
        //   add_round_key(round)
        // }
        // // Rounds 8 - 14
        // Sequential.Foreach(8 until 14 by 1) { round => 
        //   substitute_bytes()
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   if ((round % 2) == 0) {
        //     Pipe{expand_key()}
        //   }
        //   add_round_key(round)
        // }
        // // Round 14
        // Pipe {
        //   substitute_bytes()
        //   Pipe{shift_rows()}
        //   Pipe{expand_key()}
        //   add_round_key(14)
        // }


        // /* Totally pipelined version */
        // // Round 0
        // add_round_key(0)
        
        // // Round 1
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(1)
        // }

        // // Round 2
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(2)
        // }

        // // Round 3
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(3)
        // }

        // // Round 4
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(4)
        // }

        // // Round 5
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(5)
        // }

        // // Round 6
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(6)
        // }

        // // Round 7
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(7)
        // }

        // // Round 8
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(8)
        // }

        // // Round 9
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(9)
        // }

        // // Round 10
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(10)
        // }

        // // Round 11
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(11)
        // }

        // // Round 12
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   Pipe{expand_key()}
        //   add_round_key(12)
        // }

        // // Round 13
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{mix_columns()}
        //   add_round_key(13)
        // }

        // // Round 14
        // Pipe{
        //   Pipe{substitute_bytes()}
        //   Pipe{shift_rows()}
        //   Pipe{expand_key()}
        //   add_round_key(14)
        // }

        // Reshape plaintext_sram (gh issue # 83)
        val ciphertext_flat = SRAM[Int](16)
        Sequential.Foreach(4 by 1, 4 by 1) {(i,j) => 
          ciphertext_flat(j.to[I32]*4+i.to[I32]) = plaintext_sram(i,j).as[Int]
        }

        ciphertext_dram(block_id::block_id+16 par par_store) store ciphertext_flat
      }

      // // Debugging
      // key_debug store key_sram

    }

    val ciphertext = getMem(ciphertext_dram)
    val ciphertext_gold = Array.fill(args(0).to[Int])(Array[Int](142,162,183,202,81,103,69,191,234,252,73,144,75,73,96,137)).flatten

    printArray(ciphertext_gold, "Expected: ")
    printArray(ciphertext, "Got: ")

    // // Debugging
    // val key_dbg = getMem(key_debug)
    // printArray(key_dbg, "Key: ")

    val cksum = ciphertext_gold.zip(ciphertext){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (AES) * For retiming, need to fix ^ reduction if not parallelized")
    assert(cksum)
  }
}


@spatial class Viterbi extends SpatialTest {
  /*

                    ←       N_OBS            →

          State 63 ----- State 63 ----- State 63                
        /  .        \ /            \ /                     P(obs | state) = emission_probs   
       /   .         X              X                                         (N_STATES x N_TOKENS)
      /    .        / \            / \                        
     O----State k  ----- State k  ----- State k  ...            
      \    .        \ /            \ /                          
       \   .         X              X                          shortest path to (state, obs) = llike
        \  .        / \            / \                                                          (N_OBS x N_STATES)
          State 0  ----- State 0  ----- State 0                  
      ↑               ↑                                                           
    init_states     transition_probs                        
     (N_STATES)       (N_STATES x N_STATES)

            obs_vec
             (N_OBS, max value = N_TOKENS)


  TODO: Eliminate backprop step and do everything feed-forward
  MachSuite Concerns:
    - Constructing path step by step seems to give the wrong result because they do extra math in the backprop step.
           Why do you need to do math when going backwards? I thought you just read off the result

  */

  type T = FixPt[TRUE,_16,_16]


  def main(args: Array[String]): Unit = {
    // Setup dimensions of problem
    val N_STATES = 64
    val N_TOKENS = 64
    val N_OBS = 140

    // debugging
    val steps_to_take = N_OBS //ArgIn[Int] //
    // setArg(steps_to_take, args(0).to[Int])

    // Setup data
    val init_states = Array[T](4.6977033615112305.to[T],3.6915655136108398.to[T],4.8652229309082031.to[T],4.7658410072326660.to[T],
      4.0006790161132812.to[T],3.9517300128936768.to[T],3.4640796184539795.to[T],3.4600069522857666.to[T],4.2856273651123047.to[T],
      3.6522088050842285.to[T],4.8189344406127930.to[T],3.8075556755065918.to[T],3.8743767738342285.to[T],5.4135279655456543.to[T],
      4.9173111915588379.to[T],3.6458325386047363.to[T],5.8528852462768555.to[T],11.3210048675537109.to[T],4.9971127510070801.to[T],
      5.1006979942321777.to[T],3.5980830192565918.to[T],5.3161897659301758.to[T],3.4544019699096680.to[T],3.7314746379852295.to[T],
      4.9998908042907715.to[T],3.4898567199707031.to[T],4.2091164588928223.to[T],3.5122559070587158.to[T],3.9326364994049072.to[T],
      7.2767667770385742.to[T],3.6539671421051025.to[T],4.0916681289672852.to[T],3.5044839382171631.to[T],4.5234117507934570.to[T],
      3.7673256397247314.to[T],4.0265331268310547.to[T],3.7147023677825928.to[T],6.7589721679687500.to[T],3.5749390125274658.to[T],
      3.7701597213745117.to[T],3.5728175640106201.to[T],5.0258340835571289.to[T],4.9390106201171875.to[T],5.7208223342895508.to[T],
      6.3652114868164062.to[T],3.5838112831115723.to[T],5.0102572441101074.to[T],4.0017414093017578.to[T],4.2373661994934082.to[T],
      3.8841004371643066.to[T],5.3679313659667969.to[T],3.9980680942535400.to[T],3.5181968212127686.to[T],4.7306714057922363.to[T],
      5.5075111389160156.to[T],5.1880970001220703.to[T],4.8259010314941406.to[T],4.2589011192321777.to[T],5.6381106376647949.to[T],
      3.4522385597229004.to[T],3.5920252799987793.to[T],4.2071061134338379.to[T],5.0856294631958008.to[T],6.0637059211730957.to[T])

    val obs_vec = Array[Int](0,27,49,52,20,31,63,63,29,0,47,4,38,38,38,38,4,43,7,28,31,
                         7,7,7,57,2,2,43,52,52,43,3,43,13,54,44,51,32,9,9,15,45,21,
                         33,61,45,62,0,55,15,55,30,13,13,53,13,13,50,57,57,34,26,21,
                         43,7,12,41,41,41,17,17,30,41,8,58,58,58,31,52,54,54,54,54,
                         54,54,15,54,54,54,54,52,56,52,21,21,21,28,18,18,15,40,1,62,
                         40,6,46,24,47,2,2,53,41,0,55,38,5,57,57,57,57,14,57,34,37,
                         57,30,30,5,1,5,62,25,59,5,2,43,30,26,38,38)

    val raw_transitions = loadCSV1D[T](s"$DATA/viterbi/viterbi_transition.csv", "\n")
    val raw_emissions = loadCSV1D[T](s"$DATA/viterbi/viterbi_emission.csv", "\n")
    val transitions = raw_transitions.reshape(N_STATES, N_STATES)
    val emissions = raw_emissions.reshape(N_STATES, N_TOKENS)

    val correct_path = Array[Int](27,27,27,27,27,31,63,63,63,63,47,4,38,38,38,38,7,7,7,
                                  7,7,7,7,7,2,2,2,43,52,52,43,43,43,43,43,44,44,32,9,9,
                                  15,45,45,45,45,45,45,0,55,55,55,30,13,13,13,13,13,13,
                                  57,57,21,21,21,21,7,41,41,41,41,17,17,30,41,41,58,58,
                                  58,31,54,54,54,54,54,54,54,54,54,54,54,54,52,52,52,21,
                                  21,21,28,18,18,40,40,40,40,40,40,46,46,2,2,2,53,53,53,
                                  55,38,57,57,57,57,57,57,57,57,57,57,30,30,5,5,5,5,5,5,
                                  5,5,30,30,26,38,38)
    // Handle DRAMs
    val init_dram = DRAM[T](N_STATES)
    val obs_dram = DRAM[Int](N_OBS)
    val transitions_dram = DRAM[T](N_STATES,N_STATES)
    val emissions_dram = DRAM[T](N_STATES,N_TOKENS)
    // val llike_dram = DRAM[T](N_OBS,N_STATES)
    val path_dram = DRAM[Int](N_OBS)
    setMem(init_dram,init_states)
    setMem(obs_dram, obs_vec)
    setMem(transitions_dram,transitions)
    setMem(emissions_dram,emissions)


    Accel{
      // Load data structures
      val obs_sram = SRAM[Int](N_OBS)
      val init_sram = SRAM[T](N_STATES)
      val transitions_sram = SRAM[T](N_STATES,N_STATES)
      val emissions_sram = SRAM[T](N_STATES,N_TOKENS)
      val llike_sram = SRAM[T](N_OBS, N_STATES)
      val path_sram = SRAM[Int](N_OBS)

      Parallel{
        obs_sram load obs_dram
        init_sram load init_dram
        transitions_sram load transitions_dram
        emissions_sram load emissions_dram
      }

      // from --> to
      Sequential.Foreach(0 until steps_to_take) { step =>
        val obs = obs_sram(step)
        Sequential.Foreach(0 until N_STATES) { to =>
          val emission = emissions_sram(to, obs)
          val best_hop = Reg[T](0x4000)
          best_hop.reset
          Reduce(best_hop)(0 until N_STATES) { from =>
            val base = llike_sram((step-1) % N_OBS, from) + transitions_sram(from,to)
            base + emission
          } { (a,b) => mux(a < b, a, b)}
          llike_sram(step,to) = mux(step == 0, emission + init_sram(to), best_hop)
        }
      }

      // to <-- from
      Sequential.Foreach(steps_to_take-1 until -1 by -1) { step =>
        val from = path_sram(step+1)
        val min_pack = Reg[Tup2[Int, T]](pack(-1.to[Int], (0x4000).to[T]))
        min_pack.reset
        Reduce(min_pack)(0 until N_STATES){ to =>
          val jump_cost = mux(step == steps_to_take-1, 0.to[T], transitions_sram(to, from))
          val p = llike_sram(step,to) + jump_cost
          pack(to,p)
        }{(a,b) => mux(a._2 < b._2, a, b)}
        path_sram(step) = min_pack._1
      }

      // Store results
      // llike_dram store llike_sram
      path_dram store path_sram
    }

    // Get data structures
    // val llike = getMatrix(llike_dram)
    val path = getMem(path_dram)

    // Print data structures
    // printMatrix(llike, "log-likelihood")
    printArray(path, "path taken")
    printArray(correct_path, "correct path")

    // Check results
    val cksum = correct_path.zip(path){_ == _}.reduce{_&&_}
    println("PASS: " + cksum + " (Viterbi)")
    assert(cksum)
  }
}


// @spatial class Stencil2D extends SpatialTest { // ReviveMe (LineBuffer)
//   /*
//            ←    COLS     →   
//          ___________________             ___________________                         
//         |                   |           |X  X  X  X  X  X 00|          
//     ↑   |    ←3→            |           |                 00|    * this app pads right and bottom borders with 0      
//         |    ___            |           |    VALID DATA   00|          
//         |  ↑|   |           |           |X  X  X  X  X  X 00|          
//         |  3|   | ----->    |    ->     |                 00|            
//  ROWS   |  ↓|___|           |           |X  X  X  X  X  X 00|          
//         |                   |           |                 00|          
//         |                   |           |X  X  X  X  X  X 00|          
//         |                   |           |                 00|          
//     ↓   |                   |           |0000000000000000000|          
//         |                   |           |0000000000000000000|          
//          ```````````````````             ```````````````````      
                                               
                                               
//   */



//   def main(args: Array[String]): Unit = {

//     // Problem properties
//     val ROWS = 128
//     val COLS = 64
//     val filter_size = 9
//     val par_lb_load = 4

//     // Setup data
//     val raw_data = loadCSV1D[Int](s"$DATA/stencil/stencil2d_data.csv", "\n")
//     val data = raw_data.reshape(ROWS, COLS)

//     // Setup DRAMs
//     val data_dram = DRAM[Int](ROWS,COLS)
//     val result_dram = DRAM[Int](ROWS,COLS)

//     setMem(data_dram, data)

//     Accel {

//       val filter = LUT[Int](3,3)(379,909,468, // Reverse columns because we shift in from left side
//                                  771,886,165,
//                                  553,963,159)
//       val lb = LineBuffer[Int](3,COLS)
//       val sr = RegFile[Int](3,3)
//       val result_sram = SRAM[Int](ROWS,COLS)
//       Foreach(ROWS by 1){ i =>
//         val wr_row = (i-2)%ROWS
//         lb load data_dram(i, 0::COLS par par_lb_load)
//         Foreach(COLS by 1) {j =>
//           Foreach(3 by 1 par 3) {k => sr(k,*) <<= lb(k,j)}
//           val temp = Reduce(Reg[Int](0))(3 by 1, 3 by 1){(r,c) => sr(r,c) * filter(r,c)}{_+_}
//           val wr_col = (j-2)%COLS
//           if (i >= 2 && j >= 2) {result_sram(wr_row,wr_col) = temp}
//           else {result_sram(wr_row,wr_col) = 0}
//         }
//       }

//       result_dram store result_sram
//     }

//     // Get results
//     val result_data = getMatrix(result_dram)
//     val raw_gold = loadCSV1D[Int](s"$DATA/stencil/stencil2d_gold.csv", "\n")
//     val gold = raw_gold.reshape(ROWS,COLS)

//     // Printers
//     printMatrix(gold, "gold")
//     printMatrix(result_data, "result")

//     val cksum = (0::ROWS, 0::COLS){(i,j) => if (i < ROWS-2 && j < COLS-2) gold(i,j) == result_data(i,j) else true }.reduce{_&&_}
//     println("PASS: " + cksum + " (Stencil2D) * Fix modulo addresses in scalagen?")

//   }
// }


// @spatial class Stencil3D extends SpatialTest { // ReviveMe (LineBuffer)
//  /*
                                                                                                                             
//  H   ↗        ___________________                  ___________________                                                                  
//   E         /                   /|               /000000000000000000/ |                                                                
//    I       / ←    ROW      →   / |              /0  x  x  x  x    0/ 0|                        
//  ↙  G     /__________________ /  |             /0________________0/  0|                                                                 
//      H   |                   |   |            |0  X  X  X  X  X  0| x0|      
//       T  |     ___           |   |            |0                 0|  0|      
//          |    /__/|          |   |            |0   VALID DATA    0|  0|    *This app frames all borders with original value  
//    ↑     |  ↑|   ||          |   |            |0  X  X  X  X  X  0| x0|      
//          |  3|   || ----->   |   |   --->     |0                 0|  0|        
//   COL    |  ↓|___|/          |   |            |0  X  X  X  X  X  0| x0|      
//          |                   |   |            |0                 0|  0|      
//          |                   |   |            |0  X  X  X  X  X  0| x0|      
//          |                   |  /             |0                 0| 0/      
//    ↓     |                   | /              |0                 0|0/ 
//          |                   |/               |0000000000000000000|/        
//           ```````````````````                  ```````````````````      
                                                
                                                
//  */



//   def main(args: Array[String]): Unit = {

//     // Problem properties
//     val ROWS = 16 // Leading dim
//     val COLS = 32
//     val HEIGHT = 32
//     val par_load = 16
//     val par_store = 16
//     val loop_height = 2 (1 -> 1 -> 8)
//     val par_lb_load = 4 (1 -> 1 -> 16)
//     val PX = 1
//     // val num_slices = ArgIn[Int]
//     // setArg(num_slices, args(0).to[Int])
//     val num_slices = HEIGHT
//     val filter_size = 3*3*3

//     // Setup data
//     val raw_data = loadCSV1D[Int](s"$DATA/stencil/stencil3d_data.csv", "\n")
//     val data = raw_data.reshape(HEIGHT, COLS, ROWS)

//     // Setup DRAMs
//     val data_dram = DRAM[Int](HEIGHT, COLS, ROWS)
//     val result_dram = DRAM[Int](HEIGHT, COLS, ROWS)

//     setMem(data_dram, data)

//     Accel {
//       val filter = LUT[Int](3,3,3)(   0,  0,  0,
//                                       0, -1,  0,
//                                       0,  0,  0,

//                                       0, -1,  0,
//                                      -1,  6, -1,
//                                       0, -1,  0,

//                                       0,  0,  0,
//                                       0, -1,  0,
//                                       0,  0,  0)

//       val result_sram = SRAM[Int](HEIGHT,COLS,ROWS)

//       Foreach(num_slices by 1 par loop_height) { p => 
//         val temp_slice = SRAM[Int](COLS,ROWS)
//         MemReduce(temp_slice)(-1 until 2 by 1) { slice => 
//           val local_slice = SRAM[Int](COLS,ROWS)
//           Foreach(COLS+1 by 1 par PX){ i => 
//             val lb = LineBuffer[Int](3,ROWS)
//             lb load data_dram((p+slice)%HEIGHT, i, 0::ROWS par par_lb_load)
//             Foreach(ROWS+1 by 1 par PX) {j => 
//               val sr = RegFile[Int](3,3)
//               Foreach(3 by 1 par 3) {k => sr(k,*) <<= lb(k,j%ROWS)}
//               val temp = Reduce(Reg[Int](0))(3 by 1, 3 by 1){(r,c) => sr(r,c) * filter(slice+1,r,c)}{_+_}
//               // For final version, make wr_value a Mux1H instead of a unique writer per val
//               if (i == 0 || j == 0) {Pipe{}/*do nothing*/}
//               else if (i == 1 || i == COLS || j == 1 || j == ROWS) {
//                 Pipe{
//                   if (slice == 0) {local_slice(i-1, j-1) = sr(1,1); println("1 local_slice(" + {i-1} + "," + {j-1} + ") = " + sr(1,1))} // If on boundary of page, use meat only
//                   else {local_slice(i-1, j-1) = 0} // If on boundary of page, ignore bread
//                 }
//               }
//               else if (slice == 0 && (p == 0 || p == HEIGHT-1)) {local_slice(i-1,j-1) = sr(1,1); println("2 local_slice(" + {i-1} + "," + {j-1} + ") = " + sr(1,1))} // First and last page, use meat only
//               else if ((p == 0 || p == HEIGHT-1)) {local_slice(i-1,j-1) = 0} // First and last page, ignore bread
//               else {local_slice(i-1, j-1) = temp} // Otherwise write convolution result
//             }       
//           }
//           local_slice
//         }{_+_}

//         Foreach(COLS by 1, ROWS by 1){(i,j) => result_sram(p, i, j) = temp_slice(i,j)}

//       }

//       result_dram(0::HEIGHT, 0::COLS, 0::ROWS par par_store) store result_sram


//     }

//     // Get results
//     val result_data = getTensor3(result_dram)
//     val raw_gold = loadCSV1D[Int](s"$DATA/stencil/stencil3d_gold.csv", "\n")
//     val gold = raw_gold.reshape(HEIGHT,COLS,ROWS)

//     // Printers
//     printTensor3(gold, "gold") // Least significant dimension is horizontal, second-least is vertical, third least is ---- separated blocks
//     printTensor3(result_data, "results")

//     val cksum = gold.zip(result_data){_==_}.reduce{_&&_}
//     println("PASS: " + cksum + " (Stencil3D)")

//  }
// }


@spatial class NW_alg extends SpatialTest { // NW conflicts with something in spade
  override def runtimeArgs: Args = {
    "tcgacgaaataggatgacagcacgttctcgtattagagggccgcggtacaaaccaaatgctgcggcgtacagggcacggggcgctgttcgggagatcgggggaatcgtggcgtgggtgattcgccggc ttcgagggcgcgtgtcgcggtccatcgacatgcccggtcggtgggacgtgggcgcctgatatagaggaatgcgattggaaggtcggacgggtcggcgagttgggcccggtgaatctgccatggtcgat"
  }


 /*
  
  Needleman-Wunsch Genetic Alignment algorithm                                                  
  
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

  @struct case class nw_tuple(score: Int16, ptr: Int16)


  def main(args: Array[String]): Unit = {

    // FSM setup
    val traverseState = 0
    val padBothState = 1
    val doneState = 2

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
    val MATCH_SCORE = 1
    val MISMATCH_SCORE = -1
    val GAP_SCORE = -1 
    val seqa_string = args(0) //"tcgacgaaataggatgacagcacgttctcgtattagagggccgcggtacaaaccaaatgctgcggcgtacagggcacggggcgctgttcgggagatcgggggaatcgtggcgtgggtgattcgccggc"
    val seqb_string = args(1) //"ttcgagggcgcgtgtcgcggtccatcgacatgcccggtcggtgggacgtgggcgcctgatatagaggaatgcgattggaaggtcggacgggtcggcgagttgggcccggtgaatctgccatggtcgat"
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

      val score_matrix = SRAM[nw_tuple](max_length+1,max_length+1)

      // Build score matrix
      Foreach(length+1 by 1 par row_par){ r =>
        val this_body = r % row_par
        Sequential.Foreach(-this_body until length+1 by 1) { c => // Bug #151, should be able to remove previous_result reg when fixed
          val previous_result = Reg[nw_tuple]
          val update = if (r == 0) (nw_tuple(-c.as[Int16], 0)) else if (c == 0) (nw_tuple(-r.as[Int16], 1)) else {
            val match_score = mux(seqa_sram_raw(c-1) == seqb_sram_raw(r-1), MATCH_SCORE.to[Int16], MISMATCH_SCORE.to[Int16])
            val from_top = score_matrix(r-1, c).score + GAP_SCORE
            val from_left = previous_result.score + GAP_SCORE
            val from_diag = score_matrix(r-1, c-1).score + match_score
            mux(from_left >= from_top && from_left >= from_diag, nw_tuple(from_left, SKIPB), mux(from_top >= from_diag, nw_tuple(from_top,SKIPA), nw_tuple(from_diag, ALIGN)))
          }
          previous_result := update
          if (c >= 0) {score_matrix(r,c) = update}
          // score_matrix(r,c) = update
        }
      }

      // Read score matrix
      val b_addr = Reg[Int](0)
      val a_addr = Reg[Int](0)
      Parallel{b_addr := length; a_addr := length}
      val done_backtrack = Reg[Bit](false)
      FSM(0)(state => state != doneState) { state =>
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
        mux(state == traverseState && ((b_addr == 0.to[Int]) || (a_addr == 0.to[Int])), padBothState, 
          mux(seqa_fifo_aligned.isFull || seqb_fifo_aligned.isFull, doneState, state))// Safe to assume they fill at same time?
      }

      Parallel{
        Sequential{seqa_dram_aligned(0::length*2 par par_store) store seqa_fifo_aligned}
        Sequential{seqb_dram_aligned(0::length*2 par par_store) store seqb_fifo_aligned}
      }

    }

    val seqa_aligned_result = getMem(seqa_dram_aligned)
    val seqb_aligned_result = getMem(seqb_dram_aligned)
    val seqa_aligned_string = charArrayToString(seqa_aligned_result.map(_.to[U8]))
    val seqb_aligned_string = charArrayToString(seqb_aligned_result.map(_.to[U8]))

    // Pass if >75% match
    val matches = seqa_aligned_result.zip(seqb_aligned_result){(a,b) => if ((a == b) || (a == dash) || (b == dash)) 1 else 0}.reduce{_+_}
    val cksum = matches.to[Float] > 0.75.to[Float]*measured_length.to[Float]*2

    println("Result A: " + seqa_aligned_string)
    println("Result B: " + seqb_aligned_string)

    println("Found " + matches + " matches out of " + measured_length*2 + " elements")
    println("PASS: " + cksum + " (NW)")
    assert(cksum)
  }
}


@spatial class MD_KNN extends SpatialTest {

 /*
  
  Molecular Dynamics via K-nearest neighbors                                                                

                  ← N_NEIGHBORS →   
                 ___________________   
                |                   |  
            ↑   |                   |  
                |                   |  
                |                   |  
                |                   |  
      N_ATOMS   |                   |  
                |                   |  
                |                   |  
                |                   |  
            ↓   |                   |  
                |                   |  
                 ```````````````````   

                 For each atom (row), get id of its interactions (col), index into idx/y/z, compute potential energy, and sum them all up

                                                                                                           
 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]

  @struct case class XYZ(x: T, y: T, z: T)

  def main(args: Array[String]): Unit = {
    val N_ATOMS = 256
    val N_NEIGHBORS = 16 
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]
    val raw_xpos = loadCSV1D[T](s"$DATA/MD/knn_x.csv", "\n")
    val raw_ypos = loadCSV1D[T](s"$DATA/MD/knn_y.csv", "\n")
    val raw_zpos = loadCSV1D[T](s"$DATA/MD/knn_z.csv", "\n")
    val raw_interactions_data = loadCSV1D[Int](s"$DATA/MD/knn_interactions.csv", "\n")
    val raw_interactions = raw_interactions_data.reshape(N_ATOMS, N_NEIGHBORS)

    val xpos_dram = DRAM[T](N_ATOMS)
    val ypos_dram = DRAM[T](N_ATOMS)
    val zpos_dram = DRAM[T](N_ATOMS)
    val xforce_dram = DRAM[T](N_ATOMS)
    val yforce_dram = DRAM[T](N_ATOMS)
    val zforce_dram = DRAM[T](N_ATOMS)
    val interactions_dram = DRAM[Int](N_ATOMS, N_NEIGHBORS)

    setMem(xpos_dram, raw_xpos)
    setMem(ypos_dram, raw_ypos)
    setMem(zpos_dram, raw_zpos)
    setMem(interactions_dram, raw_interactions)

    Accel{
      val xpos_sram = SRAM[T](N_ATOMS)
      val ypos_sram = SRAM[T](N_ATOMS)
      val zpos_sram = SRAM[T](N_ATOMS)
      val xforce_sram = SRAM[T](N_ATOMS)
      val yforce_sram = SRAM[T](N_ATOMS)
      val zforce_sram = SRAM[T](N_ATOMS)
      val interactions_sram = SRAM[Int](N_ATOMS, N_NEIGHBORS) // Can also shrink sram and work row by row

      xpos_sram load xpos_dram
      ypos_sram load ypos_dram
      zpos_sram load zpos_dram
      interactions_sram load interactions_dram

      Foreach(N_ATOMS by 1) { atom =>
        val this_pos = XYZ(xpos_sram(atom), ypos_sram(atom), zpos_sram(atom))
        val total_force = Reg[XYZ](XYZ(0.to[T], 0.to[T], 0.to[T]))
        // total_force.reset // Probably unnecessary
        Reduce(total_force)(N_NEIGHBORS by 1) { neighbor => 
          val that_id = interactions_sram(atom, neighbor)
          val that_pos = XYZ(xpos_sram(that_id), ypos_sram(that_id), zpos_sram(that_id))
          val delta = XYZ(this_pos.x - that_pos.x, this_pos.y - that_pos.y, this_pos.z - that_pos.z)
          val r2inv = 1.0.to[T]/( delta.x*delta.x + delta.y*delta.y + delta.z*delta.z );
          // Assume no cutoff and aways account for all nodes in area
          val r6inv = r2inv * r2inv * r2inv;
          val potential = r6inv*(lj1*r6inv - lj2);
          val force = r2inv*potential;
          XYZ(delta.x*force, delta.y*force, delta.z*force)
        }{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}
        xforce_sram(atom) = total_force.x
        yforce_sram(atom) = total_force.y
        zforce_sram(atom) = total_force.z
      }
      xforce_dram store xforce_sram
      yforce_dram store yforce_sram
      zforce_dram store zforce_sram
    }

    val xforce_received = getMem(xforce_dram)
    val yforce_received = getMem(yforce_dram)
    val zforce_received = getMem(zforce_dram)
    val xforce_gold = loadCSV1D[T](s"$DATA/MD/knn_x_gold.csv", "\n")
    val yforce_gold = loadCSV1D[T](s"$DATA/MD/knn_y_gold.csv", "\n")
    val zforce_gold = loadCSV1D[T](s"$DATA/MD/knn_z_gold.csv", "\n")

    printArray(xforce_gold, "Gold x:")
    printArray(xforce_received, "Received x:")
    printArray(yforce_gold, "Gold y:")
    printArray(yforce_received, "Received y:")
    printArray(zforce_gold, "Gold z:")
    printArray(zforce_received, "Received z:")

    val margin = 0.001.to[T]
    val cksumx = xforce_gold.zip(xforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = yforce_gold.zip(yforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = zforce_gold.zip(zforce_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_KNN)")
    assert(cksum)
  }
}      

@spatial class MD_Grid extends SpatialTest {
 /*
  
  Moleckaler Dynamics via the grid, a digital frontier
                                                                             
                                                                             
                                                                             
                                                                             
                                                                             
                            ←      BLOCK_SIDE     →                        
                ↗                                                                                                
                          __________________________________   
       BLOCK_SIDE        /                                  /|  
                        /                                  / |  
        ↙              /                                  /  |  
                      /_________________________________ /   |  
                     |           b1                     |    |  
           ↑         |        ..  ..  ..                |    |  
                     |       - - - - - -                |    |  
                     |      :``::``::``:                |    |  
           B         |    b1:..::__::..:b1              |    |  
           L         |      - - /_/| - -                |    |  
           O         |     :``:|b0||:``:                |    |  
           C         |   b1:..:|__|/:..:b1              |    |  
           K         |      - - - -  - -                |    |  
           |         |     :``::``: :``:                |    |  
           S         |   b1:..::..: :..:b1              |    |  
           I         |          b1                      |    |  
           D         |                                  |   /   
           E         |                                  |  /   
                     |                                  | /    
           ↓         |                                  |/     
                      ``````````````````````````````````       * Each b0 contains up to "density" number of atoms
                                                               * For each b0, and then for each atom in b0, compute this atom's
                                                                    interactions with all atoms in the adjacent (27) b1's
                                                               * One of the b1's will actually be b0, so skip this contribution                      
                                                                                                           
 */

  // Max pos seems to be about 19
  type T = FixPt[TRUE, _12, _20]
  @struct case class XYZ(x: T, y: T, z: T) 


  def main(args: Array[String]): Unit = {

    val N_ATOMS = 256
    val DOMAIN_EDGE = 20
    val BLOCK_SIDE = 4
    val density = 10
    val density_aligned = density + (8 - (density % 8))
    val lj1 = 1.5.to[T]
    val lj2 = 2.to[T]

    val par_load = 1 // Wider data type
    val par_store = 1 // Wider data type
    val loop_grid0_x = 1 (1 -> 1 -> 16)
    val loop_grid0_y = 1 (1 -> 1 -> 16)
    val loop_grid0_z = 1 (1 -> 1 -> 16)
    val loop_grid1_x = 1 (1 -> 1 -> 16)
    val loop_grid1_y = 1 (1 -> 1 -> 16)
    val loop_grid1_z = 2 (1 -> 1 -> 16)
    val loop_p =       2 (1 -> 1 -> 16)
    val loop_q =       2 (1 -> 1 -> 16)

    val raw_npoints = Array[Int](4,4,3,4,5,5,2,1,1,8,4,8,3,3,7,5,4,5,6,2,2,4,4,3,3,4,7,2,3,2,
                                 2,1,7,1,3,7,6,3,3,4,3,4,5,5,6,4,2,5,7,6,5,4,3,3,5,4,4,4,3,2,3,2,7,5)
    val npoints_data = raw_npoints.reshape(BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE)

    val raw_dvec = loadCSV1D[T](s"$DATA/MD/grid_dvec.csv", "\n")
    // Strip x,y,z vectors from raw_dvec
    val dvec_x_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l)}
    val dvec_y_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+1)}
    val dvec_z_data = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_dvec(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+2)}

    val dvec_x_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val dvec_y_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val dvec_z_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_x_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_y_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val force_z_dram = DRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
    val npoints_dram = DRAM[Int](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE)

    setMem(dvec_x_dram, dvec_x_data)
    setMem(dvec_y_dram, dvec_y_data)
    setMem(dvec_z_dram, dvec_z_data)
    setMem(npoints_dram, npoints_data)

    Accel{
      val dvec_x_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
      val dvec_y_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
      val dvec_z_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
      val npoints_sram = SRAM[Int](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE)
      val force_x_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
      val force_y_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)
      val force_z_sram = SRAM[T](BLOCK_SIDE,BLOCK_SIDE,BLOCK_SIDE,density)

      dvec_x_sram load dvec_x_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
      dvec_y_sram load dvec_y_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
      dvec_z_sram load dvec_z_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load)
      npoints_sram load npoints_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE par par_load)

      // Iterate over each block
      'ATOM1LOOP.Foreach(BLOCK_SIDE by 1 par loop_grid0_x, BLOCK_SIDE by 1 par loop_grid0_y, BLOCK_SIDE by 1 par loop_grid0_z){(b0x,b0y,b0z) => 
        // Iterate over each point in this block, considering boundaries
        val b0_cube_forces = SRAM[XYZ](density)
        val b1x_start = max(0.to[Int],b0x-1.to[Int])
        val b1x_end = min(BLOCK_SIDE.to[Int], b0x+2.to[Int])
        val b1y_start = max(0.to[Int],b0y-1.to[Int])
        val b1y_end = min(BLOCK_SIDE.to[Int], b0y+2.to[Int])
        val b1z_start = max(0.to[Int],b0z-1.to[Int])
        val b1z_end = min(BLOCK_SIDE.to[Int], b0z+2.to[Int])
        // Iterate over points in b0
        val p_range = npoints_sram(b0x, b0y, b0z)
        println(r"\npt $b0x $b0y $b0z, atom2bounds ${b1x_start}-${b1x_end}, ${b1y_start}-${b1y_end}, ${b1z_start}-${b1z_end}")
        'ATOM2LOOP.MemReduce(b0_cube_forces)(b1x_start until b1x_end by 1, b1y_start until b1y_end by 1, b1z_start until b1z_end by 1 par loop_grid1_z) { (b1x, b1y, b1z) => 
          val b1_cube_contributions = SRAM[XYZ](density).buffer
          val q_range = npoints_sram(b1x, b1y, b1z)
          println(r"  p_range/q_range for $b0x $b0y $b0z - $b1x $b1y $b1z = ${p_range} ${q_range}")
          'PLOOP.Foreach(0 until p_range par loop_p) { p_idx =>
            val px = dvec_x_sram(b0x, b0y, b0z, p_idx)
            val py = dvec_y_sram(b0x, b0y, b0z, p_idx)
            val pz = dvec_z_sram(b0x, b0y, b0z, p_idx)
            val q_sum = Reg[XYZ](XYZ(0.to[T], 0.to[T], 0.to[T]))
            'QLOOP.Reduce(q_sum)(0 until q_range par loop_q) { q_idx => 
              val qx = dvec_x_sram(b1x, b1y, b1z, q_idx)
              val qy = dvec_y_sram(b1x, b1y, b1z, q_idx)
              val qz = dvec_z_sram(b1x, b1y, b1z, q_idx)
              val tmp = if ( !(b0x == b1x && b0y == b1y && b0z == b1z && p_idx == q_idx) ) { // Skip self
                println(r"    $b1x $b1y $b1z ${q_idx} = $qx $qy $qz")
                val delta = XYZ(px - qx, py - qy, pz - qz)
                val r2inv = 1.0.to[T]/( delta.x*delta.x + delta.y*delta.y + delta.z*delta.z );
                // Assume no cutoff and aways account for all nodes in area
                val r6inv = r2inv * r2inv * r2inv;
                val potential = r6inv*(lj1*r6inv - lj2);
                val force = r2inv*potential;
                XYZ(delta.x*force, delta.y*force, delta.z*force)
              } else {
                XYZ(0.to[T], 0.to[T], 0.to[T])
              }
              tmp
            }{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}
            println(r"    $b0x $b0y $b0z ${p_idx}: contribution of $b1x $b1y $b1z = ${q_sum}")
            b1_cube_contributions(p_idx) = q_sum
          }
          Foreach(p_range until density) { i => b1_cube_contributions(i) = XYZ(0.to[T], 0.to[T], 0.to[T]) } // Zero out untouched interactions          
          b1_cube_contributions
        }{(a,b) => XYZ(a.x + b.x, a.y + b.y, a.z + b.z)}

        Foreach(0 until density) { i => 
          println(r"total contribution @ $b0x $b0y $b0z $i = ${b0_cube_forces(i)}")
          force_x_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).x
          force_y_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).y
          force_z_sram(b0x,b0y,b0z,i) = b0_cube_forces(i).z
        }
      }
      force_x_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_x_sram
      force_y_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_y_sram
      force_z_dram(0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density par par_load) store force_z_sram

    }

    // No need to align after bug #195 fixed
    val force_x_received = getTensor4(force_x_dram)
    val force_y_received = getTensor4(force_y_dram)
    val force_z_received = getTensor4(force_z_dram)
    val raw_force_gold = loadCSV1D[T](s"$DATA/MD/grid_gold.csv", "\n")
    val force_x_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l)}
    val force_y_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+1)}
    val force_z_gold = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => raw_force_gold(i*BLOCK_SIDE*BLOCK_SIDE*density*3 + j*BLOCK_SIDE*density*3 + k*density*3 + 3*l+2)}


    printTensor4(force_x_gold, "Gold x:")
    printTensor4(force_x_received, "Received x:")
    printTensor4(force_y_gold, "Gold y:")
    printTensor4(force_y_received, "Received y:")
    printTensor4(force_z_gold, "Gold z:")
    printTensor4(force_z_received, "Received z:")


    val margin = 0.001.to[T]
    val validateZ = (0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::BLOCK_SIDE, 0::density){(i,j,k,l) => if (abs(force_z_gold(i,j,k,l) - force_z_received(i,j,k,l)) < margin) 1 else 0}
    printTensor4(validateZ, "Z matchup")
    val cksumx = force_x_gold.zip(force_x_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumy = force_y_gold.zip(force_y_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    val cksumz = force_z_gold.zip(force_z_received){case (a,b) => abs(a - b) < margin}.reduce{_&&_}
    println("X: " + cksumx + ", Y:" + cksumy + ", Z: " + cksumz)
    val cksum = cksumx && cksumy && cksumz
    println("PASS: " + cksum + " (MD_Grid)")
    assert(cksum)
  }
}      

@spatial class KMP extends SpatialTest {
  override def runtimeArgs: Args = "the"

 /*
  
  Knuth-Morris-Pratt

  Used https://www.browserling.com/tools/text-to-hex to convert string to hex, and then converted hex to dec                                                               
                           
  Machsuite, and therefore this implementation, will hang infinitely if the first char of the pattern appears later on
   in the pattern and it also gets the same WRONG answer for pattern "these" ... -____-                                                                  
                                                                                                           
 */


  def main(args: Array[String]): Unit = {
    val raw_string_data = loadCSV1D[String](s"$DATA/kmp/kmp_string.csv", "\n")
    val raw_string_pattern = args(0)      //"bull"//Array[Int](98,117,108,108)
    val raw_string = raw_string_data(0).map{c => c.to[Int8] }
    val raw_pattern = raw_string_pattern.map{c => c.to[Int8] }
    val par_load = 16
    val outer_par = 4 (1 -> 1 -> 16)
    val STRING_SIZE_NUM = raw_string.length.to[Int]
    val PATTERN_SIZE_NUM = raw_pattern.length.to[Int]
    val STRING_SIZE = ArgIn[Int]
    val PATTERN_SIZE = ArgIn[Int]
    setArg(STRING_SIZE, STRING_SIZE_NUM)
    setArg(PATTERN_SIZE, PATTERN_SIZE_NUM)
    val string_dram = DRAM[Int8](STRING_SIZE)
    val pattern_dram = DRAM[Int8](PATTERN_SIZE)
    val nmatches = ArgOut[Int]

    setMem(string_dram, raw_string)
    setMem(pattern_dram, raw_pattern)

    Accel{
      val pattern_sram = SRAM[Int8](4) // Conveniently sized
      val kmp_next = SRAM[Int](4) // Conveniently sized

      pattern_sram load pattern_dram(0::PATTERN_SIZE) // too tiny for par

      // Init kmp_next
      val k = Reg[Int](0)
      kmp_next(0) = 0
      Sequential.Foreach(1 until PATTERN_SIZE by 1) { q => 
        // val whileCond = Reg[Bit](false)
        FSM(0)(state => state != 1) { state =>
          // whileCond := (k > 0) && (pattern_sram(k) != pattern_sram(q))
          if ((k > 0) && (pattern_sram(k) != pattern_sram(q))) k := 0 // TODO: Will it always bump back to 0 in this step or should it really be kmp_next(q)?
        }{state => mux((k > 0) && (pattern_sram(k) != pattern_sram(q)), 0, 1)}
        if (pattern_sram(k) == pattern_sram(q)) {k :+= 1.to[Int]}
        kmp_next(q) = k
      }

      // Scan string portions
      val global_matches = Sequential.Reduce(Reg[Int](0))(STRING_SIZE by (STRING_SIZE/outer_par) by STRING_SIZE/outer_par par outer_par) {chunk => 
        val num_matches = Reg[Int](0)
        Pipe{num_matches := 0}
        val string_sram = SRAM[Int8](32411) // Conveniently sized
        string_sram load string_dram(chunk::chunk + (STRING_SIZE/outer_par) + (PATTERN_SIZE-1) par par_load)
        val q = Reg[Int](0)
        Sequential.Foreach(0 until STRING_SIZE/outer_par + PATTERN_SIZE-1 by 1) { i => 
          // val whileCond = Reg[Bit](false)
          FSM(0)(state => state != 1) { state =>
            // whileCond := (q > 0) && (pattern_sram(i) != pattern_sram(q))
            if ((q > 0) && (string_sram(i) != pattern_sram(q))) q := kmp_next(q)
          }{state => mux((q > 0) && (string_sram(i) != pattern_sram(q)), 0, 1)}
          Pipe{if (pattern_sram(q) == string_sram(i)) { q :+= 1 }}
          if (q >= PATTERN_SIZE) {
            Pipe{
              num_matches :+= 1
              val bump = kmp_next(q - 1)
              q := bump
            }
          }
        }
        num_matches
      }{_+_}

      nmatches := global_matches
    }

    val pattern_length = raw_string_pattern.length
    val string_length = raw_string_data.apply(0).length
    val gold_nmatches = Array.empty[Int](string_length-pattern_length)
    for (i <- 0 until string_length-pattern_length) {
      val substr = raw_string_data.apply(0).slice(i,i+pattern_length)
      if (substr == raw_string_pattern) gold_nmatches(i) = 1
      else                              gold_nmatches(i) = 0
    }
    val computed_nmatches = getArg(nmatches)

    println("Expected " + gold_nmatches.reduce{_+_} + " matches")
    println("Found " + computed_nmatches)

    val cksum = gold_nmatches.reduce{_+_} == computed_nmatches
    println("PASS: " + cksum + " (KMP) * Implement string find, string file parser, and string <-> hex <-> dec features once argon refactor is done so we can test any strings")
    assert(cksum)
  }
}      


@spatial class GEMM_NCubed extends SpatialTest {
  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val dim = 64

    val a_data = loadCSV1D[T](s"$DATA/gemm/gemm_a.csv", "\n").reshape(dim,dim)
    val b_data = loadCSV1D[T](s"$DATA/gemm/gemm_b.csv", "\n").reshape(dim,dim)

    val a_dram = DRAM[T](dim,dim)
    val b_dram = DRAM[T](dim,dim)
    val c_dram = DRAM[T](dim,dim)

    setMem(a_dram, a_data)
    setMem(b_dram, b_data)

    Accel{
      val a_sram = SRAM[T](dim,dim)
      val b_sram = SRAM[T](dim,dim)
      val c_sram = SRAM[T](dim,dim)

      a_sram load a_dram
      b_sram load b_dram

      Foreach(dim by 1) { i => 
        Foreach(dim by 1) { j => 
          val sum = Reduce(Reg[T](0))(dim by 1) { k => 
            a_sram(i,k) * b_sram(k,j)
          }{_+_}
          c_sram(i,j) = sum
        }
      }
      c_dram store c_sram
    }

    val c_gold = loadCSV1D[T](s"$DATA/gemm/gemm_gold.csv", "\n").reshape(dim,dim)
    val c_result = getMatrix(c_dram)

    printMatrix(c_gold, "C Gold: ")
    printMatrix(c_result, "C Result: ")

    val margin = 0.5.to[T]
    val cksum = c_gold.zip(c_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    println("PASS: " + cksum + " (GEMM_NCubed)")
    assert(cksum)
  }
}      

@spatial class GEMM_Blocked extends SpatialTest { // Regression (Dense) // Args: 128
  override def runtimeArgs: Args = "128"
                                                                                                  
                                                                                                  
 /*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      


         # Loops jj and kk

                                                                                jj                                       
                                                                  ...............↓............................. 
                                                                 .               .          .                 . 
                                                                 .               . ← tile → .                 . 
                                                                 .               .          .                 . 
                                                                 .      B        .          .                 . 
                                                                 .               .          .                 . 
                                                              kk .               .          .                 . 
                                                               ↳ .................__________................... 
                                                                 .               |          |                 . 
                                                                 .               | b_sram   |      ↑          . 
                                                                 .               |          |      tile       . 
                                                                 .               |          |      ↓          . 
                                                                 ................|__________|.................. 
                                                                 .               .          .                 . 
                                                                 .               .    |     .                 . 
                                                                 .               .    |     .                 . 
                                                                 .               .    ↓     .                 . 
                                                                 .               .          .                 . 
                                                                 ..............................................
                    kk ---→                                                                                           
      _______________↓____________________________                ................__________...................          
     |               |          |                 |              .               |          |                  .      ↑   
     |               |          |                 |              .               |          |                  .      |   
     |               | ← tile → |                 |              .               |          |                  .      |   
     |      A        |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .           C   |          |                  .      |   
     |               |          |                 |              .               |  c_col   |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |    
     |               |          |                 |              .               |          |                  .
     |               |          |                 |              .               |          |                  .     dim  
     |               |          |                 |              .               |          |                  .         
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |               |          |                 |              .               |          |                  .      |   
     |_______________|__________|_________________|              ................|__________|...................      ↓   
                                                                                             
                                                                  ←----------------- dim -------------------→

        # Loop i                                                          
                                          
                                                                               jj                                       
                                                                 ...............↓............................. 
                                                                .               .          .                 . 
                                                                .               . ← tile → .                 . 
                                                                .               .          .                 . 
                                                                .      B        .          .                 . 
                                                                .               .          .                 . 
                                                             kk .               .          .                 . 
                                                              ↳ .................__________................... 
                                                                .               |          |                 . 
                                                                .               | b_sram   |      ↑          . 
                                                                .               |          |      tile       . 
                                                                .               |          |      ↓          . 
                                                                ................|__________|.................. 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                ..............................................
                      kk                                                                                               
        ...............↓.............................           ..............................................          
       .               .          .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       .               . ← tile → .                 .          .               .          .                  .     
       .      A        .          .                 .          .               .          .                  .     
       .               .          .                 .          .           C   .          .                  .     
     i .               .          .                 .          .               .          .                  .     
     ↳ .................__________...................          .................__________....................     
       .               |_a_sram___|                 .          .               |__c_tmp___|                  .     
       .```````````````.          .`````````````````.          .```````````````.          .``````````````````.
       .               .    |     .                 .          .               .          .                  .     
       .               .    |     .                 .          .               .          .                  .     
       .               .    ↓     .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       .               .          .                 .          .               .          .                  .     
       ..............................................          ...............................................     
                                                                                           
                                                                

        
        # Loop k
                                                                               jj                                       
                                                                 ...............↓............................. 
                                                                .               .          .                 . 
                                                                .               . ← tile → .                 . 
                                                                .               .          .                 . 
                                                                .      B        .          .                 . 
                                                                .               .          .                 . 
                                                             kk .               .          .                 . 
                                                              ↳ .................__________................... 
                                                                .             k |          |                 . 
                                                                .             ↳ | b_sram   |      ↑          . 
                                                                .               |          |      tile       . 
                                                                .               |          |      ↓          . 
                                                                ................|__________|.................. 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                .               .          .                 . 
                                                                ..............................................
                      kk                                                                                               
        ...............↓.............................            ..............................................         
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               . ← tile → .                 .           .               .          .                  .   
       .      A        .          .                 .           .               .          .                  .   
       .               .          .                 .           .           C   .          .                  .   
     i .               .          .                 .           .               .          .                  .   
     ↳ .................__________...................           .................__________....................   
       .               |_O________|                 .           .               |__c_tmp___|                  .   
       .```````````````. ↑        .`````````````````.           .```````````````.          .``````````````````.
       .               . k  -->   .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       .               .          .                 .           .               .          .                  .   
       ..............................................           ...............................................   
                                                                                           
                                                              


            # Loop j
                                                                              jj                                       
                                                                ...............↓............................. 
                                                               .               .          .                 . 
                                                               .               . ← tile → .                 . 
                                                               .               .          .                 . 
                                                               .      B        .          .                 . 
                                                               .               .          .                 . 
                                                            kk .               .  j -->   .                 . 
                                                             ↳ .................__↓_______................... 
                                                               .             k |          |                 . 
                                                               .             ↳ |  O       |      ↑          . 
                                                               .               |          |      tile       . 
                                                               .               |  b_sram  |      ↓          . 
                                                               ................|__________|.................. 
                                                               .               .          .                 . 
                                                               .               .          .                 . 
                                                               .               .          .                 . 
                                                               .               .          .                 . 
                                                               .               .          .                 . 
                                                               ..............................................
                     kk                                                                                               
       ...............↓.............................           ..............................................         
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               . ← tile → .                 .          .               .          .                  .     
      .      A        .          .                 .          .               .          .                  .     
      .               .          .                 .          .           C   .          .                  .     
    i .               .          .                 .          .               .          .                  .     
    ↳ .................__________...................          .................__________....................     
      .               |_O________|                 .          .               |__O_-->___|                  .     
      .```````````````. ↑        .`````````````````.          .```````````````.          .``````````````````.
      .               . k        .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      .               .          .                 .          .               .          .                  .     
      ..............................................          ...............................................     
                                                                                          
                                                                
    CONCERNS: We need to figure out how HLS is actually managing the srams, or make our management better  
              We cannot do unaligned stores yet, so tilesize of 8 won't work unless we keep ts 16 of c_sram onchip                                                                                          
 */
  type T = FixPt[TRUE,_16,_16] // Fatter type so that tileSize is burst aligned


  def main(args: Array[String]): Unit = {

    val dim_arg = args(0).to[Int]
    val dim = ArgIn[Int]
    setArg(dim, dim_arg)
    val tileSize = 16 (16 -> 16 -> 128)
    val i_tileSize = 64 (64 -> 16 -> 128)
    val par_load = 1
    val par_store = 1
    val loop_jj    = 1 // (1 -> 1 -> dim/tileSize) // THIS PAR DOES NOT WORK UNTIL BUG #205 IS FIXED
    val loop_ii    = 1 // not sure if this one works
    val loop_kk    = 1 (1 -> 1 -> 8)
    val loop_i     = 1 (1 -> 1 -> 32)
    val loop_k     = 1 (1 -> 1 -> 16)
    val loop_j     = 1 (1 -> 1 -> 16)
    val reduce_col = 1 (1 -> 1 -> 16)
    val reduce_tmp = 1 (1 -> 1 -> 16)

    // val a_data = loadCSV1D[T](s"$DATA/gemm/gemm_a.csv", "\n").reshape(dim,dim)
    // val b_data = loadCSV1D[T](s"$DATA/gemm/gemm_b.csv", "\n").reshape(dim,dim)
    val a_data = (0::dim_arg,0::dim_arg){(i,j) => random[T](5)}
    val b_data = (0::dim_arg,0::dim_arg){(i,j) => random[T](5)}
    val c_init = (0::dim_arg, 0::dim_arg){(i,j) => 0.to[T]}
    val a_dram = DRAM[T](dim,dim)
    val b_dram = DRAM[T](dim,dim)
    val c_dram = DRAM[T](dim,dim)

    setMem(a_dram, a_data)
    setMem(b_dram, b_data)
    setMem(c_dram, c_init)

    Accel{

      Foreach(dim by i_tileSize par loop_ii) { ii => // this loop defenitilely cant be parallelized right now
        Foreach(dim by tileSize par loop_jj) { jj => 
          val c_col = SRAM[T](i_tileSize,tileSize)
          MemReduce(c_col(0::i_tileSize, 0::tileSize par reduce_col))(dim by tileSize par loop_kk) { kk => 
            val c_col_partial = SRAM[T](i_tileSize,tileSize)
            val b_sram = SRAM[T](tileSize,tileSize)
            b_sram load b_dram(kk::kk.to[I32]+tileSize, jj::jj.to[I32]+tileSize par par_load)
            Foreach(i_tileSize by 1 par loop_i) { i => 
              val a_sram = SRAM[T](tileSize)
              a_sram load a_dram(ii+i, kk::kk.to[I32]+tileSize)
              val c_tmp = SRAM[T](tileSize)
              MemReduce(c_tmp par reduce_tmp)(tileSize by 1 par loop_k) { k => 
                val c_tmp_partial = SRAM[T](tileSize)
                val temp_a = a_sram(k)
                Foreach(tileSize by 1 par loop_j) { j => 
                  c_tmp_partial(j) = b_sram(k, j) * temp_a
                }
                c_tmp_partial
              }{_+_}
            Foreach(tileSize by 1){cpy => c_col_partial(i,cpy) = c_tmp(cpy)}
            }
          c_col_partial
          }{_+_}
          c_dram(ii::ii.to[I32]+i_tileSize, jj::jj.to[I32]+tileSize par par_store) store c_col
        }
      }
    }

    // val c_gold = loadCSV1D[T](s"$DATA/gemm/gemm_gold.csv", "\n").reshape(dim,dim)
    val c_gold = (0::dim_arg,0::dim_arg){(i,j) => 
      Array.tabulate(dim_arg){k => a_data(i,k) * b_data(k,j)}.reduce{_+_}
    }
    val c_result = getMatrix(c_dram)

    printMatrix(c_gold, "C Gold: ")
    printMatrix(c_result, "C Result: ")

    val margin = 0.5.to[T]
    val cksum = c_gold.zip(c_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    println("PASS: " + cksum + " (GEMM_Blocked)")
    assert(cksum)
  }
}

@spatial class Sort_Merge extends SpatialTest {
 /*                                                                                                  
                              |     |                                                                                                                                                                                        
                     |        |     |                                      |     |                                                                                                                                                                                     
                     |  |     |     |  |                             |     |     |                                                                                                                                                                           
                     |  |  |  |     |  |                          |  |     |     |     |                                                                                                                                                                     
                     |  |  |  |  |  |  |                          |  |  |  |     |     |                                                                                                                                                                     
                     |  |  |  |  |  |  |  |                       |  |  |  |  |  |     |                                                                                                                                                                      
                     |  |  |  |  |  |  |  |                       |  |  |  |  |  |  |  |                                                                                                                                                                        
                     |  |  |  |  |  |  |  |                       |  |  |  |  |  |  |  |                                                                                                                                                                      
                                                                  |  |  |  |  |  |  |  |                      
   Outer FSM iter 1:  ↖↗    ↖↗    ↖↗    ↖↗      Outer FSM iter 2:  ↖.....↖     ↖.....↖                                                                                                                      
                     fifos numel = 1                               fifos numel = 2                                                            
                                                                  
                                                                                                     
                            |           |                                           |  |                                                                                                   
                         |  |           |                                        |  |  |                                                                                                   
                      |  |  |        |  |                                  |  |  |  |  |                                                                                                  
                   |  |  |  |        |  |                               |  |  |  |  |  |                                                                                                  
                   |  |  |  |     |  |  |                            |  |  |  |  |  |  |                                                                                                  
                   |  |  |  |  |  |  |  |                         |  |  |  |  |  |  |  |                                                                                                  
                   |  |  |  |  |  |  |  |                         |  |  |  |  |  |  |  |                                                                                                  
                   |  |  |  |  |  |  |  |                         |  |  |  |  |  |  |  |                                                                                                  
 Outer FSM iter 3:  ↖...........↖               Outer FSM iter 4:                                                                                                     
                   fifos numel = 4                                 Done
                                                                                                                                                                                                                   
                                                                                                                                                                           
 */


  def main(args: Array[String]): Unit = {

    val numel = 2048
    val START = 0
    val STOP = numel
    val levels = STOP-START //ArgIn[Int]
    // setArg(levels, args(0).to[Int])

    val par_load = 8
    val par_store = 8

    val raw_data = loadCSV1D[Int](s"$DATA/sort/sort_data.csv", "\n")

    val data_dram = DRAM[Int](numel)
    // val sorted_dram = DRAM[Int](numel)

    setMem(data_dram, raw_data)

    Accel{
      val data_sram = SRAM[Int](numel)
      val lower_fifo = FIFO[Int](numel/2)
      val upper_fifo = FIFO[Int](numel/2)

      data_sram load data_dram(0::numel par par_load)


      FSM(1){m => m < levels} { m =>
        FSM(START)(i => i < STOP) { i =>
          val from = i
          val mid = i+m-1
          val to = min(i+m+m-1, STOP.to[Int])
          val lower_tmp = Reg[Int](0)
          val upper_tmp = Reg[Int](0)
          Foreach(from until mid+1 by 1){ i => lower_fifo.enq(data_sram(i)) }
          Foreach(mid+1 until to+1 by 1){ j => upper_fifo.enq(data_sram(j)) }
          Pipe.II(6).Foreach(from until to+1 by 1) { k =>  // Bug # 202
          // Pipe.Foreach(from until to+1 by 1) { k =>  // Bug # 202
            data_sram(k) = 
              if (lower_fifo.isEmpty) { upper_fifo.deq() }
              else if (upper_fifo.isEmpty) { lower_fifo.deq() }
              else if (lower_fifo.peek < upper_fifo.peek) { lower_fifo.deq() }
              else { upper_fifo.deq() }
          }
        }{ i => i + m + m }
      }{ m => m + m}

      // sorted_dram store data_sram
      data_dram(0::numel par par_store) store data_sram
    }

    val sorted_gold = loadCSV1D[Int](s"$DATA/sort/sort_gold.csv", "\n")
    val sorted_result = getMem(data_dram)

    printArray(sorted_gold, "Sorted Gold: ")
    printArray(sorted_result, "Sorted Result: ")

    val cksum = sorted_gold.zip(sorted_result){_==_}.reduce{_&&_}
    // // Use the real way to check if list is sorted instead of using machsuite gold
    // val cksum = Array.tabulate(STOP-1){ i => pack(sorted_result(i), sorted_result(i+1)) }.map{a => a._1 <= a._2}.reduce{_&&_}
    println("PASS: " + cksum + " (Sort_Merge)")
    assert(cksum)
  }
}


@spatial class Sort_Radix extends SpatialTest {
 /*                                                                                                  
    TODO: Cartoon of what this is doing                                                         
                                                                                                                                                                                                                       
                                                                                                                                                                           
 */


  def main(args: Array[String]): Unit = {

    val numel = 2048
    val NUM_BLOCKS = 512
    val EL_PER_BLOCK = 4
    val RADIX = 4
    val BUCKET_SIZE = NUM_BLOCKS*RADIX
    val SCAN_BLOCK = 16
    val SCAN_RADIX = BUCKET_SIZE/SCAN_BLOCK
    val a = false.to[Bit]
    val b = true.to[Bit]

    val raw_data = loadCSV1D[Int](s"$DATA/sort/sort_data.csv", "\n")

    val data_dram = DRAM[Int](numel)

    setMem(data_dram, raw_data)

    Accel{
      val a_sram = SRAM[Int](numel)
      val b_sram = SRAM[Int](numel)
      val bucket_sram = SRAM[Int](BUCKET_SIZE)
      val sum_sram = SRAM[Int](SCAN_RADIX)
      val valid_buffer = Reg[Bit](false)
      
      a_sram load data_dram

      def hist(exp: I32, s: SRAM1[Int]): Unit = {
        Foreach(NUM_BLOCKS by 1) { blockID => 
          Sequential.Foreach(4 by 1) {i => 
            val a_indx = blockID * EL_PER_BLOCK + i
            // val a_indx = Reg[Int](0)
            // a_indx := blockID * EL_PER_BLOCK + i
            val shifted = Reg[Int](0)
            shifted := s(a_indx) // TODO: Allow just s(a_indx) >> exp syntax
            // Reduce(shifted)(exp by 1) { k => shifted >> 1}{(a,b) => b}
            Foreach(exp by 1) { k => shifted := shifted.value >> 1}
            val bucket_indx = (shifted.value & 0x03)*NUM_BLOCKS + blockID + 1
            // println(" hist bucket(" + bucket_indx + ") = " + bucket_sram(bucket_indx) + " + 1")
            // println(r"shifted started with ${s(a_indx)} (from ${a_indx}), now is ${shifted.value}, + $blockID")
            if (bucket_indx < 2048) {bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1}
          }
        }
      }

      def local_scan(): Unit = {
        Foreach(SCAN_RADIX by 1) { radixID => 
          Sequential.Foreach(1 until SCAN_BLOCK by 1) { i => // Loop carry dependency
            val bucket_indx = radixID*SCAN_BLOCK.to[Int] + i
            val prev_val = Reg[Int](0)
            Pipe{ prev_val := bucket_sram(bucket_indx - 1) }
            Pipe{ bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + prev_val }
            // println(r"local_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${prev_val}")
          }
        }
      }

      def sum_scan(): Unit = {
        sum_sram(0) = 0
        Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => // Remove manual II when bug #207 (or #151?) is fixed
        // Pipe.Foreach(1 until SCAN_RADIX by 1) { radixID => 
          val bucket_indx = radixID*SCAN_BLOCK - 1
          sum_sram(radixID) = sum_sram(radixID-1) + bucket_sram(bucket_indx)
          // println(r"sum_scan: sum_sram(${radixID}) = ${sum_sram(radixID-1)} + ${bucket_sram(bucket_indx)}")
        }
      }

      def last_step_scan(): Unit = {
        Foreach(SCAN_RADIX by 1) { radixID => 
          Foreach(SCAN_BLOCK by 1) { i => 
            val bucket_indx = radixID * SCAN_BLOCK + i
            bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + sum_sram(radixID)
            // println(r"last_step_scan: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + ${sum_sram(radixID)}")
          }
        }
      }

      def update(exp: I32, s1: SRAM1[Int], s2: SRAM1[Int]): Unit = {

        Foreach(NUM_BLOCKS by 1) { blockID => 
          Sequential.Foreach(4 by 1) { i => 
            val shifted = Reg[Int](0)
            shifted := s1(blockID*EL_PER_BLOCK + i) // TODO: Allow just s(a_indx) >> exp syntax
            // Reduce(shifted)(exp by 1) { k => shifted >> 1}{(a,b) => b}
            Foreach(exp by 1) { k => shifted := shifted >> 1}
            val bucket_indx = (shifted & 0x3)*NUM_BLOCKS + blockID
            val a_indx = blockID * EL_PER_BLOCK + i
            s2(bucket_sram(bucket_indx)) = s1(a_indx)
            bucket_sram(bucket_indx) = bucket_sram(bucket_indx) + 1
            // println(r"update: bucket_sram(${bucket_indx}) = ${bucket_sram(bucket_indx)} + 1")
          }
        }
      }


      Sequential.Foreach(32 by 2) { exp => 
        Foreach(BUCKET_SIZE by 1) { i => bucket_sram(i) = 0 }
  
        if (valid_buffer == a) {
          Pipe{hist(exp, a_sram)}
        } else {
          Pipe{hist(exp, b_sram)}
        }

        local_scan()
        sum_scan()
        last_step_scan()

        if (valid_buffer == a) {
          // println("s1 = a, s2 = b")
          Sequential{
            Pipe{update(exp, a_sram, b_sram)}
            Pipe{valid_buffer := b}
          }
        } else {
          // println("s1 = b, s2 = a")
          Sequential{
            Pipe{update(exp, b_sram, a_sram)}
            Pipe{valid_buffer := a}
          }
        }

      }

      if (valid_buffer == a) {
        // println("dumping buf a")
        data_dram store a_sram
      } else {
        // println("dumping buf b")
        data_dram store b_sram
      }

    }

    val sorted_gold = loadCSV1D[Int](s"$DATA/sort/sort_gold.csv", "\n")
    val sorted_result = getMem(data_dram)

    printArray(sorted_gold, "Sorted Gold: ")
    printArray(sorted_result, "Sorted Result: ")

    val cksum = sorted_gold.zip(sorted_result){_==_}.reduce{_&&_}
    printArray(sorted_gold.zip(sorted_result){_==_}, "Matchup: ")
    // // Use the real way to check if list is sorted instead of using machsuite gold
    // // This way says I've done goofed, issue #
    // val cksum = Array.tabulate(STOP-1){ i => pack(sorted_result(i), sorted_result(i+1)) }.map{a => a._1 <= a._2}.reduce{_&&_}
    println("PASS: " + cksum + " (Sort_Radix)")
    assert(cksum)
  }
}


@spatial class SPMV_CRS extends SpatialTest {
 /*                                                                                                  
   Sparse Matrix is the IEEE 494 bus interconnect matrix from UF Sparse Datasets   

    Datastructures in CRS:
              0__1__2__3__4__5_
      rowid: |__|__|__|__|__|__|
               |  |____   |_____________                       
              _↓_______`↓_______________`↓_______
      cols:  |___________________________________|
              _↓________↓________________↓_______
     values: |___________________________________|    

        Use cols to gather from vector:
              ____________________________________________
     vector: |____________________________________________|


   Concerns:
      Machsuite assumes everything fits on chip?  So there are no gathers...  Setting tilesize to problem size for now
 */

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val NNZ = 1666
    val N = 494
    val tileSize = 494

    val raw_values = loadCSV1D[T](s"$DATA/SPMV/crs_values.csv", "\n")
    val raw_cols = loadCSV1D[Int](s"$DATA/SPMV/crs_cols.csv", "\n")
    val raw_rowid = loadCSV1D[Int](s"$DATA/SPMV/crs_rowid.csv", "\n")
    val raw_vec = loadCSV1D[T](s"$DATA/SPMV/crs_vec.csv", "\n")

    val values_dram = DRAM[T](NNZ) 
    val cols_dram = DRAM[Int](NNZ) 
    val rowid_dram = DRAM[Int](N+1) 
    val vec_dram = DRAM[T](N) 
    val result_dram = DRAM[T](N)

    val par_load = 16
    val par_segment_load = 1 // Do not change
    val par_store = 1 // Do not change
    val tile_par = 1 (1 -> 1 -> 16) // Do not change
    val pt_par = 1 (1 -> 1 -> 16) // Do not change?
    val red_par = 2 (1 -> 1 -> 16)

    setMem(values_dram, raw_values)
    setMem(cols_dram, raw_cols)
    setMem(rowid_dram, raw_rowid)
    setMem(vec_dram, raw_vec)

    Accel {
      Foreach(N/tileSize by 1 par tile_par) { tile =>
        val rowid_sram = SRAM[Int](tileSize+1)
        val result_sram = SRAM[T](tileSize)

        rowid_sram load rowid_dram(tile*(tileSize+1) :: (tile+1)*(tileSize+1) par par_load)
        Foreach(tileSize by 1 par pt_par) { i => 
          val cols_sram = SRAM[Int](tileSize)
          val values_sram = SRAM[T](tileSize)
          val vec_sram = SRAM[T](tileSize)

          val start_id = rowid_sram(i)
          val stop_id = rowid_sram(i+1)
          Parallel{
            cols_sram load cols_dram(start_id :: stop_id par par_segment_load)
            values_sram load values_dram(start_id :: stop_id par par_segment_load)
          }
          vec_sram gather vec_dram(cols_sram, stop_id - start_id)
          println("row " + {i + tile})
          val element = Reduce(Reg[T](0))(stop_id - start_id by 1 par red_par) { j => 
            // println(" partial from " + j + " = " + {values_sram(j) * vec_sram(j)})
            values_sram(j) * vec_sram(j)
          }{_+_}
          result_sram(i) = element
        }
        result_dram(tile*tileSize :: (tile+1)*tileSize par par_store) store result_sram
      }
    }

    val data_gold = loadCSV1D[T](s"$DATA/SPMV/crs_gold.csv", "\n")
    val data_result = getMem(result_dram)

    printArray(data_gold, "Gold: ")
    printArray(data_result, "Result: ")

    val margin = 0.2.to[T] // Scala does not stay in bounds as tightly as chisel
    val cksum = data_gold.zip(data_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}

    println("PASS: " + cksum + " (SPMV_CRS) * Fix gather on arbitrary width elements (64 for better prec here), issue #126")
    assert(cksum)
  }
}

@spatial class SPMV_ELL extends SpatialTest {
 /*                                                                                                  
   Sparse Matrix is the IEEE 494 bus interconnect matrix from UF Sparse Datasets   

    Datastructures in Ellpack:

                                                                                                                                                 
                                             ←   L    →
        _____________________                __________         _________                                                                              
       | 9  0  2  0  0  0  1 |          ↑   | 0  2  6  |       | 9  2  1 |                                                                             
       |                     |              |          |       |         |                                                                             
       | 3  0  0  0  0  2  0 | ===>     N   | 0  5  *  |       | 3  2  * |                                                                             
       |                     |              |          |       |         |                                                                             
       | 0  0  0  0  5  0  0 |          ↓   | 4  *  *  |       | 5  *  * |                                                                             
        `````````````````````                ``````````         `````````                                                                              
         uncompressed                        cols               data                                                                                             
                                                                                       
 */

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val NNZ = 1666
    val N = 494
    val L = 10    
    val tileSize = N

    val raw_values = loadCSV1D[T](s"$DATA/SPMV/ell_values.csv", "\n").reshape(N,L)
    val raw_cols = loadCSV1D[Int](s"$DATA/SPMV/ell_cols.csv", "\n").reshape(N,L)
    val raw_vec = loadCSV1D[T](s"$DATA/SPMV/ell_vec.csv", "\n")

    val values_dram = DRAM[T](N,L) 
    val cols_dram = DRAM[Int](N,L) 
    val vec_dram = DRAM[T](N) 
    val result_dram = DRAM[T](N)

    setMem(values_dram, raw_values)
    setMem(cols_dram, raw_cols)
    setMem(vec_dram, raw_vec)

    Accel {
      Foreach(N/tileSize by 1){ tile => 
        val cols_sram = SRAM[Int](tileSize, L)
        val values_sram = SRAM[T](tileSize, L)
        val result_sram = SRAM[T](tileSize)

        cols_sram load cols_dram(tile::tile+tileSize, 0::L)
        values_sram load values_dram(tile::tile+tileSize, 0::L)

        Foreach(tileSize by 1) { i => 
          val vec_sram = SRAM[T](L)
          val gather_addrs = SRAM[Int](L)
          Foreach(L by 1) { j => gather_addrs(j) = cols_sram(i, j) }
          vec_sram gather vec_dram(gather_addrs) //vec_sram gather vec_dram(gather_addrs, L)
          val element = Reduce(Reg[T](0))(L by 1) { k => values_sram(i,k) * vec_sram(k) }{_+_}
          result_sram(i) = element
        }

        result_dram(tile::tile+tileSize) store result_sram

      }
    }

    val data_gold = loadCSV1D[T](s"$DATA/SPMV/ell_gold.csv", "\n")
    val data_result = getMem(result_dram)

    printArray(data_gold, "Gold: ")
    printArray(data_result, "Result: ")
    printArray(data_result.zip(data_gold){_-_}, "delta")

    val margin = 0.2.to[T] // Scala does not stay in bounds as tightly as chisel
    val cksum = data_gold.zip(data_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}

    println("PASS: " + cksum + " (SPMV_ELL)")
    assert(cksum)
  }
}


@spatial class Backprop extends SpatialTest {
  override def runtimeArgs: Args = "5"

 /*                                                                                                  
    Concerns: 
        Their implementation and way of writing this app really sucks...
 */

  type T = FixPt[TRUE,_32,_32]
  // type T = Float

  def main(args: Array[String]): Unit = {

    val input_dimension =  13
    val possible_outputs =  3
    val training_sets =   163
    val iters = ArgIn[Int]
    setArg(iters, args(0).to[Int])
    // setArg(iters, training_sets)
    // setArg(iters, 20)
    val nodes_per_layer =  64
    val layers =            2
    val learning_rate =  0.01.to[T]
    val epochs =            1
    val test_sets =        15
    val norm_param =    0.005

    val par_load = 8
    val par_store = 8
    val fw_layer1_pt    = 2 (1 -> 1 -> 16)
    val fw_layer1_red   = 2 (1 -> 1 -> 16)
    val fw_layer1_relu  = 2 (1 -> 1 -> 16)
    val fw_layer2_pt    = 2 (1 -> 1 -> 16)
    val fw_layer2_red   = 2 (1 -> 1 -> 16)
    val fw_layer2_relu  = 2 (1 -> 1 -> 16)
    val fw_layer3_pt    = 2 (1 -> 1 -> 16)
    val fw_layer3_red   = 2 (1 -> 1 -> 16)
    val fw_layer3_relu  = 2 (1 -> 1 -> 16)
    val softmax_red     = 2 (1 -> 1 -> 16)
    val softmax_nmlz    = 2 (1 -> 1 -> 16)
    val err_par         = 2 (1 -> 1 -> 16)
    val bw_act2_red     = 2 (1 -> 1 -> 16)
    val bw_act2_pt      = 2 (1 -> 1 -> 16)
    val bw_act1_red     = 2 (1 -> 1 -> 16)
    val bw_act1_pt      = 2 (1 -> 1 -> 16)
    val bw_layer3       = 2 (1 -> 1 -> 16)
    val bw_layer2       = 2 (1 -> 1 -> 16)
    val bw_layer1       = 2 (1 -> 1 -> 16)
    val ud_weight1      = 2 (1 -> 1 -> 16)
    val ud_bias1        = 2 (1 -> 1 -> 16)
    val ud_weight1_norm = 2 (1 -> 1 -> 16)
    val ud_bias1_norm   = 2 (1 -> 1 -> 16)
    val ud_weight2      = 2 (1 -> 1 -> 16)
    val ud_bias2        = 2 (1 -> 1 -> 16)
    val ud_weight2_norm = 2 (1 -> 1 -> 16)
    val ud_bias2_norm   = 2 (1 -> 1 -> 16)
    val ud_weight3      = 2 (1 -> 1 -> 16)
    val ud_bias3        = 2 (1 -> 1 -> 16)
    val ud_weight3_norm = 2 (1 -> 1 -> 16)
    val ud_bias3_norm   = 2 (1 -> 1 -> 16)

    val weights1_data = loadCSV1D[T](s"$DATA/backprop/backprop_weights1.csv").reshape(input_dimension, nodes_per_layer)
    val weights2_data = loadCSV1D[T](s"$DATA/backprop/backprop_weights2.csv").reshape(nodes_per_layer, nodes_per_layer)
    val weights3_data = loadCSV1D[T](s"$DATA/backprop/backprop_weights3.csv")//.reshape(nodes_per_layer, possible_outputs)
    val biases1_data = loadCSV1D[T](s"$DATA/backprop/backprop_bias1.csv")
    val biases2_data = loadCSV1D[T](s"$DATA/backprop/backprop_bias2.csv")
    val biases3_data = Array[T](0.255050659180.to[T],0.018173217773.to[T],-0.353927612305.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T],0.to[T])
    val training_data = loadCSV1D[T](s"$DATA/backprop/backprop_training_data.csv")//.reshape(training_sets, input_dimension)
    val training_targets_data = loadCSV1D[T](s"$DATA/backprop/backprop_training_targets.csv")//.reshape(training_sets, possible_outputs)

    val weights1_dram = DRAM[T](input_dimension, nodes_per_layer)
    val weights2_dram = DRAM[T](nodes_per_layer, nodes_per_layer)
    val weights3_dram = DRAM[T](nodes_per_layer*possible_outputs)
    // val weights3_dram_aligned = DRAM[T](nodes_per_layer, possible_outputs + (16 - possible_outputs))
    val biases1_dram = DRAM[T](nodes_per_layer)
    val biases2_dram = DRAM[T](nodes_per_layer)
    val biases3_dram = DRAM[T](16)
    val training_data_dram = DRAM[T](training_sets*input_dimension)
    val training_targets_dram = DRAM[T](training_sets*possible_outputs)

    setMem(weights1_dram, weights1_data)
    setMem(weights2_dram, weights2_data)
    setMem(weights3_dram, weights3_data)
    setMem(biases1_dram, biases1_data)
    setMem(biases2_dram, biases2_data)
    setMem(biases3_dram, biases3_data)
    setMem(training_data_dram, training_data)
    setMem(training_targets_dram, training_targets_data)

    Accel{

      def RELU(x: T): T = {
        // 1.0.to[T]/(1.0.to[T]+exp_taylor(-x))
        mux(x < 0.to[T], 0.to[T], x)
        // mux(x < -2, 0, mux(x < 2, x*0.25.to[T] + 0.5.to[T], 1))
      }

      val biases1_sram = SRAM[T](nodes_per_layer)
      val biases2_sram = SRAM[T](nodes_per_layer)
      val biases3_sram = SRAM[T](16) // burst aligned
      // val biases3_sram_aligned = SRAM[T](possible_outputs + (16 - possible_outputs)) // burst aligned
      val weights1_sram = SRAM[T](input_dimension, nodes_per_layer) 
      val weights2_sram = SRAM[T](nodes_per_layer, nodes_per_layer)
      val weights3_sram = SRAM[T](nodes_per_layer, possible_outputs)
      val weights3_sram_flat = SRAM[T](nodes_per_layer*possible_outputs)

      biases1_sram load biases1_dram(0::nodes_per_layer par par_load)
      biases2_sram load biases2_dram(0::nodes_per_layer par par_load)
      biases3_sram load biases3_dram(0::16 par par_load)
      weights1_sram load weights1_dram(0::input_dimension, 0::nodes_per_layer par par_load)
      weights2_sram load weights2_dram(0::nodes_per_layer, 0::nodes_per_layer par par_load)
      weights3_sram_flat load weights3_dram(0::possible_outputs*nodes_per_layer par par_load)

      // Reshape things
      Foreach(nodes_per_layer by 1, possible_outputs by 1) {(i,j) => weights3_sram(i,j) = weights3_sram_flat(i*possible_outputs + j)}

      Sequential.Foreach(iters by 1) { ii => 
        val i = ii % training_sets
        val activations1 = SRAM[T](nodes_per_layer)
        val activations2 = SRAM[T](nodes_per_layer)
        val activations3 = SRAM[T](possible_outputs)
        val dactivations1 = SRAM[T](nodes_per_layer)
        val dactivations2 = SRAM[T](nodes_per_layer)
        val dactivations3 = SRAM[T](possible_outputs)
        val training_sram = SRAM[T](input_dimension + (16 - input_dimension) + 2)
        val training_targets = SRAM[T](possible_outputs + (16 - possible_outputs))
        val net_outputs = SRAM[T](possible_outputs)
        val delta_outputs = SRAM[T](possible_outputs)
        val delta_weights1 = SRAM[T](input_dimension, nodes_per_layer)
        val delta_weights2 = SRAM[T](nodes_per_layer,nodes_per_layer)
        val delta_weights3 = SRAM[T](nodes_per_layer, possible_outputs)
        val oracle_activations1 = SRAM[T](nodes_per_layer)
        val oracle_activations2 = SRAM[T](nodes_per_layer)

        training_sram load training_data_dram(i*input_dimension::(i+1)*input_dimension par par_load)
        training_targets load training_targets_dram(i*possible_outputs::(i+1)*possible_outputs)

        def print_bias1(): Unit = {
          println("starting bias1:")
          Foreach(nodes_per_layer by 1) { i => println("bias1@ " + i + " = " + biases1_sram(i))}
        }
        def print_bias2(): Unit = {
          println("starting bias2:")
          Foreach(nodes_per_layer by 1) { i => println("bias2@ " + i + " = " + biases2_sram(i))}
        }
        def print_bias3(): Unit = {
          println("starting bias3:")
          Foreach(possible_outputs by 1) { i => println("bias3@ " + i + " = " + biases3_sram(i))}
        }
        def print_activations1(): Unit = {
          println("starting activations1:")
          Foreach(nodes_per_layer by 1) { i => println("act1@ " + i + " = " + activations1(i))}
        }
        def print_dactivations1(): Unit = {
          println("starting dactivations1:")
          Foreach(nodes_per_layer by 1) { i => println("dact1@ " + i + " = " + dactivations1(i))}
        }
        def print_activations2(): Unit = {
          println("starting activations2:")
          Foreach(nodes_per_layer by 1) { i => println("act2@ " + i + " = " + activations2(i))}
        }
        def print_dactivations2(): Unit = {
          println("starting dactivations2:")
          Foreach(nodes_per_layer by 1) { i => println("dact2@ " + i + " = " + dactivations2(i))}
        }
        def print_activations3(): Unit = {
          println("starting activations3:")
          Foreach(possible_outputs by 1) { i => println("act3@ " + i + " = " + activations3(i))}
        }
        def print_dactivations3(): Unit = {
          println("starting dactivations3:")
          Foreach(possible_outputs by 1) { i => println("dact3@ " + i + " = " + dactivations3(i))}
        }
        def print_weights3(): Unit = {
          println("starting weights3:")
          Foreach(nodes_per_layer by 1, possible_outputs by 1) { (i,j) => println("weights3@ " + i + "," + j + " = " + weights3_sram(i,j))}
        }
        def print_weights1(): Unit = {
          println("starting weights1:")
          Foreach(input_dimension by 1, nodes_per_layer by 1) { (i,j) => println("weights1@ " + i + "," + j + " = " + weights1_sram(i,j))}
        }
        def print_oracle2(): Unit = {
          println("starting oracle2:")
          Foreach(nodes_per_layer by 1) { i => println("oracle2@ " + i + " = " + oracle_activations2(i))}
        }
        def print_oracle1(): Unit = {
          println("starting oracle1:")
          Foreach(nodes_per_layer by 1) { i => println("oracle1@ " + i + " = " + oracle_activations1(i))}
        }
        def print_netoutputs(): Unit = {
          println("starting netoutputs:")
          Foreach(possible_outputs by 1) { i => println("netout@ " + i + " = " + net_outputs(i))}
        }
        def print_delta(): Unit = {
          println("starting delta:")
          Foreach(possible_outputs by 1) { i => println("delta@ " + i + " = " + delta_outputs(i))}
        }

        // println("\n\nIter " + i)

        def forward_pass(): Unit = {
          // Input Layer 
          Foreach(nodes_per_layer by 1 par fw_layer1_pt){ j => // Pretty sure machsuite indexes into their weights1 array wrong here
            activations1(j) = Reduce(Reg[T](0))(input_dimension by 1 par fw_layer1_red) { i => weights1_sram(i, j) * training_sram(i)}{_+_} + biases1_sram(j)
          }
          // print_activations1()

          // Relu
          Sequential.Foreach(nodes_per_layer by 1 par fw_layer1_relu) { i => 
            Pipe{dactivations1(i) = activations1(i)*(1.0.to[T]-activations1(i))}
            Pipe{activations1(i) = RELU(activations1(i))}
          }
          // print_dactivations1()
          // print_activations1()
       
          // Middle layer
          Foreach(nodes_per_layer by 1 par fw_layer2_pt){ j => 
            activations2(j) = Reduce(Reg[T](0))(nodes_per_layer by 1 par fw_layer2_red) { i => weights2_sram(i, j) * activations1(i)}{_+_} + biases2_sram(j)
          }
          // print_activations2()
          
          // Relu
          Sequential.Foreach(nodes_per_layer by 1 par fw_layer2_relu) { i => 
            Pipe{dactivations2(i) = activations2(i)*(1.0.to[T]-activations2(i))}
            Pipe{activations2(i) = RELU(activations2(i))}
          }
          // print_dactivations2()
          // print_activations2()
          
          // Last layer
          // print_weights3()
          // print_bias3()
          Foreach(possible_outputs by 1 par fw_layer3_pt){ j => 
            activations3(j) = Reduce(Reg[T](0))(nodes_per_layer by 1 par fw_layer3_red) { i => weights3_sram(i, j) * activations2(i)}{_+_} + biases3_sram(j)
          }
          // print_activations3()

          // Relu
          Sequential.Foreach(possible_outputs by 1 par fw_layer3_relu) { i => 
            Pipe{dactivations3(i) = activations3(i)*(1.0.to[T]-activations3(i))}
            Pipe{activations3(i) = RELU(activations3(i))}
          }

          // print_dactivations3()
          // print_activations3()
          
          // Softmax
          val normalize = Reduce(Reg[T](0))(possible_outputs by 1 par softmax_red) { i => exp_taylor(-activations3(i)) }{_+_}
          Foreach(possible_outputs by 1 par softmax_nmlz) { i => net_outputs(i) = exp_taylor(-activations3(i))/normalize }
          // print_netoutputs()
        }

        def compute_error(): Unit = {
          // Compute output error
          Foreach(possible_outputs by 1 par err_par) { i => delta_outputs(i) = -(net_outputs(i) - training_targets(i)) * dactivations3(i) }
        }

        def backward_pass(): Unit = {
          // Delta weights on last layer
          Foreach(nodes_per_layer by 1, possible_outputs by 1 par bw_layer3){(i,j) => delta_weights3(i,j) = activations2(i) * delta_outputs(j)}

          // Oracle activation 2
          // print_netoutputs()
          // print_delta()
          Sequential.Foreach(nodes_per_layer by 1 par bw_act2_pt) { i => oracle_activations2(i) = dactivations2(i) * Reduce(Reg[T](0))(possible_outputs by 1 par bw_act2_red){ j => delta_outputs(j) * weights3_sram(i,j) }{_+_}}
          // print_oracle2()

          // Delta weights on middle layer
          Foreach(nodes_per_layer by 1, nodes_per_layer by 1 par bw_layer2) { (i,j) => delta_weights2(i,j) = activations1(i) - oracle_activations2(j) }

          // Oracle activation 1
          Sequential.Foreach(nodes_per_layer by 1 par bw_act1_pt) { i => oracle_activations1(i) = dactivations1(i) * Reduce(Reg[T](0))(nodes_per_layer by 1 par bw_act1_red) { j => oracle_activations2(j) * weights2_sram(i,j) }{_+_} }
          // print_oracle1()

          // Delta weights on input layer
          Foreach(input_dimension by 1, nodes_per_layer by 1 par bw_layer1) { (i,j) => delta_weights1(i,j) = oracle_activations1(j) * training_sram(i) }
        }

        def update_weights(): Unit = {
          // Update input layer weights
          val norm_temp1 = Sequential.Reduce(Reg[T](0))(input_dimension by 1, nodes_per_layer by 1 par ud_weight1_norm){ (i,j) => 
            Pipe{weights1_sram(i,j) = weights1_sram(i,j) - delta_weights1(i,j) * learning_rate}
            weights1_sram(i,j) * weights1_sram(i,j)
          }{_+_}
          // print_weights1()
          // print_bias1()
          val bias_norm_temp1 = Sequential.Reduce(Reg[T](0))(nodes_per_layer by 1 par ud_bias1_norm) { i => 
            Pipe{biases1_sram(i) = biases1_sram(i) - (oracle_activations1(i)*learning_rate)}
            biases1_sram(i) * biases1_sram(i)
          }{_+_}
          // print_bias1()
          val norm1 = sqrt_approx(norm_temp1.value)
          val bias_norm1 = sqrt_approx(bias_norm_temp1.value)

          Foreach(input_dimension by 1, nodes_per_layer by 1 par ud_weight1){ (i,j) => 
            weights1_sram(i,j) = weights1_sram(i,j) / norm1
          }
          // println("normalize 1 " + norm1)
          // print_weights1()
          Foreach(nodes_per_layer by 1 par ud_bias1) {i => biases1_sram(i) = biases1_sram(i)/bias_norm1}
          // print_bias1()

          // Update middle layer weights
          val norm_temp2 = Sequential.Reduce(Reg[T](0))(nodes_per_layer by 1, nodes_per_layer by 1 par ud_weight2_norm){ (i,j) => 
            Pipe{weights2_sram(i,j) = weights2_sram(i,j) - delta_weights2(i,j) * learning_rate}
            weights2_sram(i,j) * weights2_sram(i,j)
          }{_+_}
          val bias_norm_temp2 = Sequential.Reduce(Reg[T](0))(nodes_per_layer by 1 par ud_bias2_norm) { i => 
            Pipe{biases2_sram(i) = biases2_sram(i) - (oracle_activations2(i)*learning_rate)}
            biases2_sram(i) * biases2_sram(i)
          }{_+_}
          // print_bias2()

          val norm2 = sqrt_approx(norm_temp2.value)
          val bias_norm2 = sqrt_approx(bias_norm_temp2.value)

          Foreach(nodes_per_layer by 1, nodes_per_layer by 1 par ud_weight2){ (i,j) => 
            weights2_sram(i,j) = weights2_sram(i,j) / norm2
          }
          Foreach(nodes_per_layer by 1 par ud_bias2) {i => biases2_sram(i) = biases2_sram(i)/bias_norm2}
          // print_bias2()

          // Update last layer weights
          val norm_temp3 = Sequential.Reduce(Reg[T](0))(nodes_per_layer by 1, possible_outputs by 1 par ud_weight3_norm){ (i,j) => 
            Pipe{weights3_sram(i,j) = weights3_sram(i,j) - delta_weights3(i,j) * learning_rate}
            weights3_sram(i,j) * weights3_sram(i,j)
          }{_+_}
          val bias_norm_temp3 = Sequential.Reduce(Reg[T](0))(possible_outputs by 1 par ud_bias3_norm) { i => 
            Pipe{biases3_sram(i) = biases3_sram(i) - (delta_outputs(i)*learning_rate)}
            biases3_sram(i) * biases3_sram(i)
          }{_+_}

          val norm3 = sqrt_approx(norm_temp3.value)
          val bias_norm3 = sqrt_approx(bias_norm_temp3.value)

          // print_bias3()
          Foreach(nodes_per_layer by 1, possible_outputs by 1 par ud_weight3){ (i,j) => 
            weights3_sram(i,j) = weights3_sram(i,j) / norm3
          }
          Foreach(possible_outputs by 1 par ud_bias3) {i => biases3_sram(i) = biases3_sram(i)/bias_norm3}
          // print_bias3()
        }

        // Algorithm:
        forward_pass()
        compute_error()
        backward_pass()
        update_weights()

      }

      // Reshape things
      Foreach(nodes_per_layer by 1, possible_outputs by 1) {(i,j) => weights3_sram_flat(i*possible_outputs + j) = weights3_sram(i,j)}

      biases1_dram(0::nodes_per_layer par par_store) store biases1_sram
      biases2_dram(0::nodes_per_layer par par_store) store biases2_sram
      biases3_dram(0::16 par par_store) store biases3_sram
      weights1_dram(0::input_dimension, 0::nodes_per_layer par par_store) store weights1_sram
      weights2_dram(0::nodes_per_layer, 0::nodes_per_layer par par_store) store weights2_sram
      weights3_dram(0::possible_outputs*nodes_per_layer par par_store) store weights3_sram_flat
    }

    val weights1_result = getMatrix(weights1_dram)
    val weights2_result = getMatrix(weights2_dram)
    val weights3_result = getMem(weights3_dram).reshape(nodes_per_layer, possible_outputs)
    // val weights3_result = (0::nodes_per_layer, 0::possible_outputs){(i,j) => weights3_result_aligned(i*possible_outputs + j)}
    val biases1_result = getMem(biases1_dram)
    val biases2_result = getMem(biases2_dram)
    val biases3_result_aligned = getMem(biases3_dram)
    val biases3_result = Array.tabulate(possible_outputs){ i => biases3_result_aligned(i) }

    // // Store these results as gold - USE AT YOUR OWN RISK
    // writeCSV1D[T](weights1_result.flatten, s"$DATA/backprop/backprop_weights1_gold.csv", "\n")
    // writeCSV1D[T](weights2_result.flatten, s"$DATA/backprop/backprop_weights2_gold.csv", "\n")
    // writeCSV1D[T](weights3_result.flatten, s"$DATA/backprop/backprop_weights3_gold.csv", "\n")
    // writeCSV1D[T](biases1_result, s"$DATA/backprop/backprop_bias1_gold.csv", "\n")
    // writeCSV1D[T](biases2_result, s"$DATA/backprop/backprop_bias2_gold.csv", "\n")
    // writeCSV1D[T](biases3_result, s"$DATA/backprop/backprop_bias3_gold.csv", "\n")

    val weights1_gold = loadCSV1D[T](s"$DATA/backprop/backprop_weights1_gold.csv", "\n").reshape(input_dimension, nodes_per_layer)
    val weights2_gold = loadCSV1D[T](s"$DATA/backprop/backprop_weights2_gold.csv", "\n").reshape(nodes_per_layer, nodes_per_layer)
    val weights3_gold = loadCSV1D[T](s"$DATA/backprop/backprop_weights3_gold.csv", "\n").reshape(nodes_per_layer, possible_outputs)
    val biases1_gold = loadCSV1D[T](s"$DATA/backprop/backprop_bias1_gold.csv")
    val biases2_gold = loadCSV1D[T](s"$DATA/backprop/backprop_bias2_gold.csv")
    val biases3_gold = loadCSV1D[T](s"$DATA/backprop/backprop_bias3_gold.csv")

    printMatrix(weights1_gold, "Gold weights 1:")
    printMatrix(weights1_result, "Result weights 1:")
    println("")
    println("")
    println("")
    printMatrix(weights2_gold, "Gold weights 2:")
    printMatrix(weights2_result, "Result weights 2:")
    println("")
    println("")
    println("")
    printMatrix(weights3_gold, "Gold weights 3:")
    printMatrix(weights3_result, "Result weights 3:")
    println("")
    println("")
    println("")
    printArray(biases1_gold, "Gold biases 1:")
    printArray(biases1_result, "Result biases 1:")
    println("")
    println("")
    println("")
    printArray(biases2_gold, "Gold biases 2:")
    printArray(biases2_result, "Result biases 2:")
    println("")
    println("")
    println("")
    printArray(biases3_gold, "Gold biases 3:")
    printArray(biases3_result, "Result biases 3:")
    println("")
    println("")
    println("")

    val margin = 0.75.to[T]
    val cksumW1 = if (weights1_gold.zip(weights1_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    val cksumW2 = if (weights2_gold.zip(weights2_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    val cksumW3 = if (weights3_gold.zip(weights3_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    val cksumB1 = if (biases1_gold.zip(biases1_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    val cksumB2 = if (biases2_gold.zip(biases2_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    val cksumB3 = if (biases3_gold.zip(biases3_result){(a,b) => abs(a-b) < margin}.reduce{_&&_}) 1 else 0
    println("Results: W1 " + cksumW1 + ", W2 " + cksumW2 + ", W3 " + cksumW3 + ", B1 " + cksumB1 + ", B2 " + cksumB2 + ", B3 " + cksumB3)

    val cksum = (cksumW1 + cksumW2 + cksumW3 + cksumB1 + cksumB2 + cksumB3) > 1
    println("PASS: " + cksum + " (Backprop) * seems like this may be saturating, need to revisit when floats are implemented, and add full 163 training points")
    assert(cksum)
  }
}


@spatial class FFT_Strided extends SpatialTest {

 /*                                                                                                  

   NOTES: This version uses one extra loop than the machsuite implementation because they mutate their counter that holds "odd" inside of the loop,
          so we can either use an FSM or use strict loops that take this mutation into account and I chose the latter
 */

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val FFT_SIZE = 1024
    val numiter = (scala.math.log(FFT_SIZE) / scala.math.log(2)).to[Int]

    val data_real = loadCSV1D[T](s"$DATA/fft/fft_strided_real.csv", "\n")
    val data_img = loadCSV1D[T](s"$DATA/fft/fft_strided_img.csv", "\n")
    val data_twid_real = loadCSV1D[T](s"$DATA/fft/fft_strided_twidreal.csv", "\n")
    val data_twid_img = loadCSV1D[T](s"$DATA/fft/fft_strided_twidimg.csv", "\n")

    val data_real_dram = DRAM[T](FFT_SIZE)
    val data_img_dram = DRAM[T](FFT_SIZE)
    val data_twid_real_dram = DRAM[T](FFT_SIZE/2)
    val data_twid_img_dram = DRAM[T](FFT_SIZE/2)
    val result_real_dram = DRAM[T](FFT_SIZE)
    val result_img_dram = DRAM[T](FFT_SIZE)

    setMem(data_real_dram, data_real)
    setMem(data_img_dram, data_img)
    setMem(data_twid_real_dram, data_twid_real)
    setMem(data_twid_img_dram, data_twid_img)

    Accel{
      val data_real_sram = SRAM[T](FFT_SIZE)
      val data_img_sram = SRAM[T](FFT_SIZE)
      val data_twid_real_sram = SRAM[T](FFT_SIZE/2)
      val data_twid_img_sram = SRAM[T](FFT_SIZE/2)

      data_real_sram load data_real_dram
      data_img_sram load data_img_dram
      data_twid_real_sram load data_twid_real_dram
      data_twid_img_sram load data_twid_img_dram

      val span = Reg[Int](FFT_SIZE)
      Foreach(0 until numiter) { log => 
        span := span >> 1
        val num_sections = Reduce(Reg[Int](1))(0 until log){i => 2}{_*_}
        Foreach(0 until num_sections) { section => 
          val base = span*(2*section+1)
          Sequential.Foreach(0 until span by 1) { offset => 
            val odd = base + offset
            val even = odd ^ span

            val rtemp = data_real_sram(even) + data_real_sram(odd)
            Pipe{data_real_sram(odd) = data_real_sram(even) - data_real_sram(odd)}
            Pipe{data_real_sram(even) = rtemp}

            val itemp = data_img_sram(even) + data_img_sram(odd)
            Pipe{data_img_sram(odd) = data_img_sram(even) - data_img_sram(odd)}
            Pipe{data_img_sram(even) = itemp}
            
            val rootindex = (Reduce(Reg[Int](1))(0 until log){i => 2.to[Int]}{_*_} * even) & (FFT_SIZE - 1).to[Int]
            if (rootindex > 0.to[Int]) {
              // println("Accessing " + rootindex + " at " + even )
              val temp = data_twid_real_sram(rootindex) * data_real_sram(odd) - data_twid_img_sram(rootindex) * data_img_sram(odd)
              data_img_sram(odd) = data_twid_real_sram(rootindex) * data_img_sram(odd) + data_twid_img_sram(rootindex) * data_real_sram(odd)
              data_real_sram(odd) = temp
            }
          }
        }
      }
      result_real_dram store data_real_sram
      result_img_dram store data_img_sram
    }

    val result_real = getMem(result_real_dram)
    val result_img = getMem(result_img_dram)
    val gold_real = loadCSV1D[T](s"$DATA/fft/fft_strided_real_gold.csv", "\n")
    val gold_img = loadCSV1D[T](s"$DATA/fft/fft_strided_img_gold.csv", "\n")

    printArray(gold_real, "Gold real: ")
    printArray(result_real, "Result real: ")
    printArray(gold_img, "Gold img: ")
    printArray(result_img, "Result img: ")

    val margin = 0.01.to[T]
    val cksumR = gold_real.zip(result_real){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksumI = gold_img.zip(result_img){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksum = cksumR && cksumI
    println("PASS: " + cksum + " (FFT_Strided)")
    assert(cksum)
  }
}

@spatial class FFT_Transpose extends SpatialTest {
 /*                                                                                                  
    Concerns: Not sure why machsuite makes a data_x and DATA_x when they only dump values from one row of DATA_x to data_x and back
              Also, is their algorithm even correct?!  It's very suspicion and I can even comment out some of their code and it still passes....
 */

  type T = FixPt[TRUE,_16,_16]

  def main(args: Array[String]): Unit = {

    val dim = 512
    val THREADS = 64
    val stride = THREADS
    val M_SQRT1_2 = 0.70710678118654752440.to[T]
    val TWOPI = 6.28318530717959.to[T]

    val data_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x.csv", "\n").reshape(8,stride)
    val data_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y.csv", "\n").reshape(8,stride)

    val work_x_dram = DRAM[T](8,stride)
    val work_y_dram = DRAM[T](8,stride)
    val result_x_dram = DRAM[T](8,stride)
    val result_y_dram = DRAM[T](8,stride)

    setMem(work_x_dram, data_x)
    setMem(work_y_dram, data_y)

    Accel{
      val work_x_sram = SRAM[T](8,stride)
      val work_y_sram = SRAM[T](8,stride)
      val smem = SRAM[T](8*8*9)

      work_x_sram load work_x_dram
      work_y_sram load work_y_dram

      val reversed_LUT = LUT[Int](8)(0,4,2,6,1,5,3,7)

      def twiddles8(tid: I32, i: Int, N: Int): Unit = {
        Sequential.Foreach(1 until 8 by 1) { j => 
          val phi = -TWOPI*(i.to[T]*reversed_LUT(j).to[T] / N.to[T])
          val phi_shifted = phi + TWOPI/2
          val beyond_left = phi_shifted < -TWOPI.to[T]/4
          val beyond_right = phi_shifted > TWOPI.to[T]/4
          val phi_bounded = mux(beyond_left, phi_shifted + TWOPI.to[T]/2, mux(beyond_right, phi_shifted - TWOPI.to[T]/2, phi_shifted))
          val phi_x = cos_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // cos(real phi)
          val phi_y = sin_taylor(phi_bounded) * mux(beyond_left || beyond_right, 1.to[T], -1.to[T]) // sin(real phi)
          val temp_x = work_x_sram(j, tid)
          Pipe{work_x_sram(j, tid) = temp_x * phi_x - work_y_sram(j, tid) * phi_y}
          Pipe{work_y_sram(j, tid) = temp_x * phi_y + work_y_sram(j, tid) * phi_x}
        }
      }

      def FFT2(tid: I32, id0: Int, id1: Int):Unit = {
        val temp_x = work_x_sram(id0, tid)
        val temp_y = work_y_sram(id0, tid)
        Pipe{work_x_sram(id0, tid) = temp_x + work_x_sram(id1, tid)}
        Pipe{work_y_sram(id0, tid) = temp_y + work_y_sram(id1, tid)}
        Pipe{work_x_sram(id1, tid) = temp_x - work_x_sram(id1, tid)}
        Pipe{work_y_sram(id1, tid) = temp_y - work_y_sram(id1, tid)}
      }

      def FFT4(tid: I32, base: Int):Unit = {
        val exp_LUT = LUT[T](2)(0, -1)
        Sequential.Foreach(0 until 2 by 1) { j => 
          Pipe{FFT2(tid, base+j.to[I32], 2+base+j.to[I32])}
        }
        val temp_x = work_x_sram(base+3,tid)
        Pipe{work_x_sram(base+3,tid) = temp_x * exp_LUT(0) - work_y_sram(base+3,tid)*exp_LUT(1)}
        Pipe{work_y_sram(base+3,tid) = temp_x * exp_LUT(1) - work_y_sram(base+3,tid)*exp_LUT(0)}
        Sequential.Foreach(0 until 2 by 1) { j => 
          Pipe{FFT2(tid, base+2*j.to[I32], 1+base+2*j.to[I32])}
        }
      }

      def FFT8(tid: I32):Unit = {
        Sequential.Foreach(0 until 4 by 1) { i => 
          Pipe{FFT2(tid, i, 4+i.to[I32])}
        }
        Sequential.Foreach(0 until 3 by 1) { i => 
          val exp_LUT = LUT[T](2,3)( 1,  0, -1,
                                      -1, -1, -1)
          val temp_x = work_x_sram(5+i.to[I32], tid)
          val mul_factor = mux(i.to[I32] == 1, 1.to[T], M_SQRT1_2)
          Pipe{work_x_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(0,i) - work_y_sram(5+i.to[I32],tid) * exp_LUT(1,i))*mul_factor}
          Pipe{work_y_sram(5+i.to[I32], tid) = (temp_x * exp_LUT(1,i) + work_y_sram(5+i.to[I32],tid) * exp_LUT(0,i))*mul_factor}
        }
        // FFT4
        Sequential.Foreach(0 until 2 by 1) { ii =>
          val base = 4*ii.to[I32]
          FFT4(tid, base)
        }

      }

      // Loop 1
      Sequential.Foreach(THREADS by 1) { tid => 
        FFT8(tid)
        twiddles8(tid, tid, dim)
      }

      val shuffle_lhs_LUT = LUT[Int](8)(0,4,1,5,2,6,3,7)
      val shuffle_rhs_LUT = LUT[Int](8)(0,1,4,5,2,3,6,7)

      // Loop 2
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 66
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8+lo // * here but >> above????
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_x_sram(rhs_factor, tid)
        }
      }

      // Loop 3
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = lo*66 + hi
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor*sx+offset)
        }
      }

      // Loop 4
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 66
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8+lo // * here but >> above????
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor*sx + offset) = work_y_sram(rhs_factor, tid)
        }
      }

      // Loop 5
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 8
        val hi = tid.to[I32] >> 3;
        val lo = tid.to[I32] & 7;
        val offset = lo*66+hi
        Sequential.Foreach(8 by 1) { i => 
          work_y_sram(i, tid) = smem(i.to[I32]*sx+offset)
        }
      }

      // Loop 6 
      Sequential.Foreach(THREADS by 1) { tid => 
        FFT8(tid)
        val hi = tid.to[I32] >> 3
        twiddles8(tid, hi, 64)
      }

      // Loop 7
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 72
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8 + lo
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_x_sram(rhs_factor, tid)
        }
      }

      // Loop 8
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*72 + lo
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_lhs_LUT(i) // [sic]
          work_x_sram(lhs_factor, tid) = smem(rhs_factor * sx + offset)
        }
      }

      // Loop 9
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 72
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*8 + lo
        Sequential.Foreach(8 by 1) { i => 
          val lhs_factor = shuffle_lhs_LUT(i)
          val rhs_factor = shuffle_rhs_LUT(i)
          smem(lhs_factor * sx + offset) = work_y_sram(rhs_factor, tid)
        }
      }

      // Loop 10
      Sequential.Foreach(THREADS by 1) { tid => 
        val sx = 8
        val hi = tid.to[I32] >> 3
        val lo = tid.to[I32] & 7
        val offset = hi*72 + lo
        Sequential.Foreach(8 by 1) { i => 
          work_y_sram(i, tid) = smem(i.to[I32] * sx + offset)
        }
      }

      // Loop 11
      Sequential.Foreach(THREADS by 1) { tid => 
        FFT8(tid)
        // Do the indirect "reversing" (LUT = 0,4,2,6,1,5,3,7)
        val tmem_x = SRAM[T](8)
        val tmem_y = SRAM[T](8)
        Foreach(8 by 1) { i => 
          Pipe{tmem_x(reversed_LUT(i)) = work_x_sram(i, tid)}
          Pipe{tmem_y(reversed_LUT(i)) = work_y_sram(i, tid)}
        }
        Foreach(8 by 1) { i => 
          Pipe{work_x_sram(i, tid) = tmem_x(i)}
          Pipe{work_y_sram(i, tid) = tmem_y(i)}
        }
      }

      result_x_dram store work_x_sram
      result_y_dram store work_y_sram
    }

    val result_x = getMatrix(result_x_dram)
    val result_y = getMatrix(result_y_dram)
    // val gold_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x_gold.csv", "\n").reshape(8,stride)
    // val gold_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y_gold.csv", "\n").reshape(8,stride)
    val gold_x = loadCSV1D[T](s"$DATA/fft/fft_transpose_x_gold.csv", "\n").reshape(8,stride)
    val gold_y = loadCSV1D[T](s"$DATA/fft/fft_transpose_y_gold.csv", "\n").reshape(8,stride)

    printMatrix(gold_x, "Gold x: ")
    println("")
    printMatrix(result_x, "Result x: ")
    println("")
    printMatrix(gold_y, "Gold y: ")
    println("")
    printMatrix(result_y, "Result y: ")
    println("")

    // printMatrix(gold_x.zip(result_x){(a,b) => abs(a-b)}, "X Diff")

    val margin = 0.5.to[T]
    val cksumX = gold_x.zip(result_x){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksumY = gold_y.zip(result_y){(a,b) => abs(a-b) < margin}.reduce{_&&_}
    val cksum = cksumX && cksumY
    println("X cksum: " + cksumX + ", Y cksum: " + cksumY)
    println("PASS: " + cksum + " (FFT_Transpose)")
    assert(cksum)
  }
}


@spatial class BFS_Bulk extends SpatialTest {
 /*                                                                                                  

       * Scan levels straight through, and index into nodes if it matches current horizon
             ___________________
    levels: |     0             |  starts as -1
             `````|`````````````
                  |
             _____🡓_____________
    nodes:  |                   |  contains start and end indices into edges
             `````|`````````````
                  |______
                       /  \
             _________↙_____🡖__________________________________________
    edges:  |                                                          |
             ``````````````````````````````````````````````````````````
        * I32 into levels and  mark each of these nodes as having depth horizon+1 if it hasn't been visited yet

  CONCERNS: This isn't really sparse...

 */

  @struct case class node_info(start: Int, end: Int)  


  def main(args: Array[String]): Unit = {
    val SCALE = 8
    val EDGE_FACTOR = 16
    val N_NODES = 1 << SCALE
    val N_EDGES = N_NODES*EDGE_FACTOR
    val N_LEVELS = 10
    val unvisited = -1
    val start_id = 38

    val nodes_raw = loadCSV1D[Int](s"$DATA/bfs/bfs_nodes.csv", "\n")
    val edges_data = loadCSV1D[Int](s"$DATA/bfs/bfs_edges.csv", "\n")

    val node_starts_data = Array.tabulate[Int](N_NODES){i => nodes_raw(2*i)}
    val node_ends_data = Array.tabulate[Int](N_NODES){i => nodes_raw(2*i+1)}
    val node_starts_dram = DRAM[Int](N_NODES)
    val node_ends_dram = DRAM[Int](N_NODES)
    val edges_dram = DRAM[Int](N_EDGES)
    val widths_dram = DRAM[Int](16)

    setMem(node_starts_dram, node_starts_data)
    setMem(node_ends_dram, node_ends_data)
    setMem(edges_dram, edges_data)

    Accel{

      val node_starts_sram = SRAM[Int](N_NODES)
      val node_ends_sram = SRAM[Int](N_NODES)
      val levels_sram = SRAM[Int](N_NODES)
      val edges_sram = SRAM[Int](N_NODES) // bigger than necessary
      val widths_sram = SRAM[Int](16)

      node_starts_sram load node_starts_dram
      node_ends_sram load node_ends_dram

      Foreach(N_NODES by 1){ i => levels_sram(i) = unvisited }
      Pipe{levels_sram(start_id) = 0}
      Foreach(16 by 1) {i => widths_sram(i) = if ( i == 0) 1 else 0}
      val level_width = Reg[Int](0)
      FSM(0)(horizon => horizon < N_LEVELS) { horizon =>
        // level_width.reset
        level_width := 0
        Sequential.Reduce(level_width)(N_NODES by 1) { n => 
          val node_width = Reg[Int](0)
          // node_width.reset
          node_width := 0
          if (levels_sram(n) == horizon) {
            Pipe{
              val start = node_starts_sram(n)
              val end = node_ends_sram(n)
              val length = end - start
              edges_sram load edges_dram(start::end)
              Sequential.Reduce(node_width)(length by 1) { e =>
                val tmp_dst = edges_sram(e)
                val dst_level = levels_sram(tmp_dst)
                if (dst_level == unvisited) { levels_sram(tmp_dst) = horizon+1 }
                mux(dst_level == unvisited, 1, 0)
              }{_+_}
            }
          }
          node_width
        }{_+_}
        widths_sram(horizon+1) = level_width
      }{ horizon => mux(horizon > 0 && level_width == 0.to[Int], N_LEVELS+1, horizon+1) }

      widths_dram store widths_sram

    }

    val widths_gold = Array[Int](1,26,184,22,0,0,0,0,0,0,0,0,0,0,0,0)
    val widths_result = getMem(widths_dram)

    printArray(widths_gold, "Gold: ")
    printArray(widths_result, "Received: ")

    val cksum = widths_gold.zip(widths_result){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (BFS_Bulk)")
    assert(cksum)
  }
}


@spatial class BFS_Queue extends SpatialTest {
 /*                                                                                                  
          ________________
    Q:   |          x x x |
          `````````↑``````
                   * Grab numel before starting next horizon so we know how many to deq before we hit next frontier
             ___________________
    levels: |     0             |  starts as -1
             `````|`````````````
                  |
             _____🡓_____________
    nodes:  |                   |  contains start and end indices into edges
             `````|`````````````
                  |______
                       /  \
             _________↙_____🡖__________________________________________
    edges:  |                                                          |
             ``````````````````````````````````````````````````````````
        * I32 into levels push them onto queue if they are unvisited

  CONCERNS: This isn't really sparse...

 */


  def main(args: Array[String]): Unit = {

    val SCALE = 8
    val EDGE_FACTOR = 16
    val N_NODES = 1 << SCALE
    val N_EDGES = N_NODES*EDGE_FACTOR
    val N_LEVELS = 10
    val unvisited = -1
    val start_id = 38

    val nodes_raw = loadCSV1D[Int](s"$DATA/bfs/bfs_nodes.csv", "\n")
    val edges_data = loadCSV1D[Int](s"$DATA/bfs/bfs_edges.csv", "\n")

    val node_starts_data = Array.tabulate[Int](N_NODES){i => nodes_raw(2*i)}
    val node_ends_data = Array.tabulate[Int](N_NODES){i => nodes_raw(2*i+1)}
    val node_starts_dram = DRAM[Int](N_NODES)
    val node_ends_dram = DRAM[Int](N_NODES)
    val edges_dram = DRAM[Int](N_EDGES)
    val widths_dram = DRAM[Int](16)

    setMem(node_starts_dram, node_starts_data)
    setMem(node_ends_dram, node_ends_data)
    setMem(edges_dram, edges_data)

    Accel{
      val node_starts_sram = SRAM[Int](N_NODES)
      val node_ends_sram = SRAM[Int](N_NODES)
      val levels_sram = SRAM[Int](N_NODES)
      val edges_sram = SRAM[Int](N_NODES) // bigger than necessary
      val Q = FIFO[Int](N_NODES)
      val widths_sram = SRAM[Int](16)

      node_starts_sram load node_starts_dram
      node_ends_sram load node_ends_dram

      Foreach(N_NODES by 1){ i => levels_sram(i) = unvisited }
      Pipe{levels_sram(start_id) = 0}
      Foreach(16 by 1) {i => widths_sram(i) = if ( i == 0) 1 else 0}
      Q.enq(start_id)

      FSM(0)( horizon => horizon < N_LEVELS ) { horizon =>
        val level_size = Q.numel
        Sequential.Foreach(level_size by 1) { i => 
          val n = Q.deq()
          val start = node_starts_sram(n)
          val end = node_ends_sram(n)
          val length = end - start
          edges_sram load edges_dram(start::end)
          Sequential.Foreach(length by 1) { e =>
            val tmp_dst = edges_sram(e)
            val dst_level = levels_sram(tmp_dst)
            if (dst_level == unvisited) { Q.enq(tmp_dst) }
            if (dst_level == unvisited) { levels_sram(tmp_dst) = horizon+1 }
          }
        }
        widths_sram(horizon+1) = Q.numel
      }{ horizon => mux(Q.numel == 0.to[Int], N_LEVELS+1, horizon+1) }

      widths_dram store widths_sram

    }

    val widths_gold = Array[Int](1,26,184,22,0,0,0,0,0,0,0,0,0,0,0,0)
    val widths_result = getMem(widths_dram)

    printArray(widths_gold, "Gold: ")
    printArray(widths_result, "Received: ")

    val cksum = widths_gold.zip(widths_result){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (BFS_Queue)")
    assert(cksum)
  }
}

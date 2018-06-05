package spatial.tests.apps

import spatial.dsl._
import spatial.targets._

@test class SHA1 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type ULong = FixPt[FALSE, _32, _0]
  @struct case class byte_pack(a: Int8, b: Int8, c: Int8, d: Int8)


  def main(args: Array[String]): Unit = {
    // Setup off-chip data
    val BLOCK_SIZE = 8192
    val SHA_BLOCKSIZE = 64

    val PX = 1 

    val CONST1 = 0x5a827999L
    val CONST2 = 0x6ed9eba1L
    val CONST3 = 0x8f1bbcdcL
    val CONST4 = 0xca62c1d6L

    val raw_text = loadCSV1D[String](s"$DATA/machsuite/sha_txt.csv", "\n").apply(0)
    val data_text = raw_text.map{c => c.to[Int8] }
    val len = ArgIn[Int]
    setArg(len, data_text.length)
    val text_dram = DRAM[Int8](len)
    val hash_dram = DRAM[ULong](16)//(5)

    // println("Hashing: " + charArrayToString(data_text) + " (len: " + data_text.length + ")")
    println("Hashing: " + raw_text + " (len: " + data_text.length + ")")
    // printArray(data_text, "text as data: ")
    setMem(text_dram, data_text)

    Accel{
      val buffer = SRAM[Int8](BLOCK_SIZE)
      val sha_digest = RegFile[ULong](5, List(0x67452301L.to[ULong], 0xefcdab89L.to[ULong], 
                                              0x98badcfeL.to[ULong], 0x10325476L.to[ULong], 
                                              0xc3d2e1f0L.to[ULong])
                                     )
      val sha_data = SRAM[ULong](16)
      val count_lo = Reg[Int](0)
      val count_hi = Reg[Int](0)

      def asLong(r: Reg[ULong]): ULong = {r.value.bits(31::0).as[ULong]}

      def sha_transform(): Unit = {
        val W = SRAM[ULong](80)
        val A = Reg[ULong]
        val B = Reg[ULong]
        val C = Reg[ULong]
        val D = Reg[ULong]
        val E = Reg[ULong]

        Foreach(80 by 1 par PX) { i =>
                  W(i) = if (i < 16) {sha_data(i)} else {W(i.as[I32]-3) ^ W(i.as[I32]-8) ^ W(i.as[I32]-14) ^ W(i.as[I32]-16)}
        }

        A := sha_digest(0)
        B := sha_digest(1)
        C := sha_digest(2)
        D := sha_digest(3)
        E := sha_digest(4)

        Foreach(20 by 1) { i => 
          val temp = ((A << 5) | (A >> (32 - 5))) + E + W(i) + CONST1 + ((B & C) | (~B & D))
          E := D; D := C; C := ((B << 30) | (B >> (32 - 30))); B := A; A := temp
        }
        Foreach(20 until 40 by 1) { i => 
          val temp = ((A << 5) | (A >> (32 - 5))) + E + W(i) + CONST2 + (B ^ C ^ D)
          E := D; D := C; C := ((B << 30) | (B >> (32 - 30))); B := A; A := temp
        }
        Foreach(40 until 60 by 1) { i => 
          val temp = ((A << 5) | (A >> (32 - 5))) + E + W(i) + CONST3 + ((B & C) | (B & D) | (C & D))
          E := D; D := C; C := ((B << 30) | (B >> (32 - 30))); B := A; A := temp
        }
        Foreach(60 until 80 by 1) { i => 
          val temp = ((A << 5) | (A >> (32 - 5))) + E + W(i) + CONST4 + (B ^ C ^ D)
          E := D; D := C; C := ((B << 30) | (B >> (32 - 30))); B := A; A := temp
        }

        Pipe{sha_digest(0) = sha_digest(0) + A}
        // Pipe{println("sha_digest 0 is " + sha_digest(0))}
        Pipe{sha_digest(1) = sha_digest(1) + B}
        Pipe{sha_digest(2) = sha_digest(2) + C}
        Pipe{sha_digest(3) = sha_digest(3) + D}
        Pipe{sha_digest(4) = sha_digest(4) + E}
      }
        def sha_update(count: I32): Unit = {
        if (count_lo + (count << 3) < count_lo) {count_hi :+= 1}
        count_lo :+= count << 3
        count_hi :+= count >> 29
        Foreach(0 until count by SHA_BLOCKSIZE) { base => 
            val numel = min(count - base, SHA_BLOCKSIZE.to[I32])
          // TODO: Can make this one writer only
          if (numel == SHA_BLOCKSIZE) {Pipe{
            Foreach(SHA_BLOCKSIZE/4 by 1){ i => 
                      sha_data(i) = (buffer(base + i.to[I32]*4).as[ULong]) | (buffer(base + i.to[I32]*4+1).as[ULong] << 8) | (buffer(base + i.to[I32]*4 + 2).as[ULong] << 16) | (buffer(base + i.to[I32]*4+3).as[ULong] << 24)
            }
            sha_transform()
          }} else {
            Foreach(0 until numel by 1) { i => 
                      sha_data(i) = (buffer(base + i.to[I32]*4).as[ULong]) | (buffer(base + i.to[I32]*4+1).as[ULong] << 8) | (buffer(base + i.to[I32]*4 + 2).as[ULong] << 16) | (buffer(base + i.to[I32]*4+3).as[ULong] << 24)
            }         
          }
        }

      }

      // Pipe{sha_digest(0) = 0x67452301L.to[ULong]}
      // Pipe{sha_digest(1) = 0xefcdab89L.to[ULong]}
      // Pipe{sha_digest(2) = 0x98badcfeL.to[ULong]}
      // Pipe{sha_digest(3) = 0x10325476L.to[ULong]}
      // Pipe{sha_digest(4) = 0xc3d2e1f0L.to[ULong]}

      Sequential.Foreach(len by BLOCK_SIZE) { chunk => 
        val count = min(BLOCK_SIZE.to[Int], (len - chunk))
        buffer load text_dram(chunk::chunk+count)
        sha_update(count)

        // def byte_reverse(x: ULong): ULong = {
        //  byte_pack(x(31::24).as[Int8],x(23::16).as[Int8],x(15::8).as[Int8],x(7::0).as[Int8]).as[ULong]
        // }

        // Final sha
        // TODO: This last bit is probably wrong for any input that is not size 8192
        val lo_bit_count = count_lo.value.to[ULong]
        val hi_bit_count = count_hi.value.to[ULong]
        val count_final = ((lo_bit_count.to[Int8] >> 3) & 0x3f.to[Int8]).to[Int]
        sha_data(count_final) = 0x80
        if (count_final > 56) {
          Foreach(count_final+1 until 16 by 1) { i => sha_data(i) = 0 }
          Sequential(sha_transform())
          sha_data(14) = 0
        } else {
          Foreach(count_final+1 until 16 by 1) { i => sha_data(i) = 0 }
        }
        Pipe{sha_data(14) = hi_bit_count}
        Pipe{sha_data(15) = lo_bit_count}
        sha_transform()
      }


      hash_dram store sha_digest
    }

    val hashed_result = getMem(hash_dram)
    val hashed_gold = Array[ULong](1754467640L,1633762310L,3755791939L,3062269980L,2187536409L,0,0,0,0,0,0,0,0,0,0,0)
    printArray(hashed_gold, "Expected: ")
    printArray(hashed_result, "Got: ")

    val cksum = hashed_gold == hashed_result
    println("PASS: " + cksum + " (SHA1)")
    assert(cksum)
  }
}


@test class JPEG_Markers extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type UInt8 = FixPt[FALSE, _8, _0]
  type UInt2 = FixPt[FALSE, _2, _0]
  type UInt16 = FixPt[FALSE, _16, _0]
  type UInt = FixPt[FALSE, _32, _0]
  @struct case class comp_struct(index: UInt8,
                                 id: UInt8,
                                 h_samp_factor: UInt8,
                                 v_samp_factor: UInt8,
                                 quant_tbl_no: UInt8
                                )
  @struct case class comp_acdc(index: UInt8,
                               dc: UInt8,
                               ac: UInt8
                              )


  def main(args: Array[String]): Unit = {

    val M_SOI = 216 // Start of image
    val M_SOF0 = 192 // Baseline DCT ( Huffman ) 
    val M_SOS = 218 // Start of Scan ( Head of Compressed Data )
    val M_DHT = 196
    val M_DQT = 219
    val M_EOI = 217
    val DCTSIZE2 = 64
    val NUM_COMPONENT = 3
    val NUM_HUFF_TBLS = 2
    val RGB_NUM = 3
    val SF4_1_1 = 2
    val SF1_1_1 = 0

    // Validity check values
    val out_length_get_sof = 17
    val out_data_precision_get_sof = 8
    val out_p_jinfo_image_height_get_sof = 59
    val out_p_jinfo_image_width_get_sof = 90
    val out_p_jinfo_num_components_get_sof = 3

    val jpg_data = loadCSV1D[UInt8](s"$DATA/machsuite/jpeg_input.csv", ",")
    val numel = jpg_data.length
    assert(!(jpg_data(0) != 255.to[UInt8] || jpg_data(1) != M_SOI.to[UInt8]), "Not a jpeg file!")

    val jpg_size = ArgIn[Int]
    setArg(jpg_size, numel)
    val jpg_dram = DRAM[UInt8](jpg_size)
    setMem(jpg_dram, jpg_data)

    // Intermediate results
    val image_valid = ArgOut[Boolean]
    val errors_found = ArgOut[Int]
    val smp_fact = ArgOut[UInt2]
    val jpeg_data_start = ArgOut[Int]
    val quant_tbl_quantval = DRAM[UInt16](4,DCTSIZE2)
    val dc_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS,64) // 36)
    val dc_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS,320) // 257)
    val ac_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS,64) // 36)
    val ac_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS,320) // 257)
    val component_ac_0 = ArgOut[UInt8]
    val component_dc_0 = ArgOut[UInt8]
    val component_ac_1 = ArgOut[UInt8]
    val component_dc_1 = ArgOut[UInt8]
    val component_ac_2 = ArgOut[UInt8]
    val component_dc_2 = ArgOut[UInt8]
    // val comp_dram = DRAM[comp_struct](NUM_COMPONENT)
    // val comp_dram_acdc = DRAM[comp_acdc](NUM_COMPONENT)

    Accel{
      val jpg_sram = FIFO[UInt8](5207)

      val component = SRAM[comp_struct](NUM_COMPONENT)
      val component_acdc = SRAM[comp_acdc](NUM_COMPONENT)
      val izigzag_index = LUT[Int](64)( 0.to[Int], 1.to[Int], 8.to[Int], 16.to[Int], 9.to[Int], 2.to[Int], 3.to[Int], 10.to[Int],
                                    17.to[Int], 24.to[Int], 32.to[Int], 25.to[Int], 18.to[Int], 11.to[Int], 4.to[Int], 5.to[Int],
                                    12.to[Int], 19.to[Int], 26.to[Int], 33.to[Int], 40.to[Int], 48.to[Int], 41.to[Int], 34.to[Int],
                                    27.to[Int], 20.to[Int], 13.to[Int], 6.to[Int], 7.to[Int], 14.to[Int], 21.to[Int], 28.to[Int],
                                    35.to[Int], 42.to[Int], 49.to[Int], 56.to[Int], 57.to[Int], 50.to[Int], 43.to[Int], 36.to[Int],
                                    29.to[Int], 22.to[Int], 15.to[Int], 23.to[Int], 30.to[Int], 37.to[Int], 44.to[Int], 51.to[Int],
                                    58.to[Int], 59.to[Int], 52.to[Int], 45.to[Int], 38.to[Int], 31.to[Int], 39.to[Int], 46.to[Int],
                                    53.to[Int], 60.to[Int], 61.to[Int], 54.to[Int], 47.to[Int], 55.to[Int], 62.to[Int], 63.to[Int]
                                  )

      val errors = Reg[Int](0)
      val eoi_found = Reg[Boolean](false)
      val p_jinfo_quant_tbl_quantval = SRAM[UInt16](4,DCTSIZE2)
      val p_jinfo_jpeg_data = Reg[Int](0)
      val p_jinfo_dc_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_dc_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 320) // 257)
      val p_jinfo_ac_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_ac_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 320) // 257)

      val p_jinfo_smp_fact = Reg[UInt2](0)
      val p_jinfo_num_components = Reg[UInt8](0)
      val p_jinfo_image_height = Reg[UInt16](0)
      val p_jinfo_image_width = Reg[UInt16](0)
      val p_jinfo_MCUHeight = Reg[UInt16](0)
      val p_jinfo_MCUWidth = Reg[UInt16](0)
      val p_jinfo_NumMCU = Reg[UInt16](0)

      def read_word(): UInt16 = {
        val tmp1 = Reg[UInt8]
        val tmp2 = Reg[UInt8]
        val tmp = Reg[UInt16]
        Pipe{tmp1 := jpg_sram.deq()}
        Pipe{tmp2 := jpg_sram.deq()}
        Pipe{tmp := (tmp1.value.as[UInt16] << 8) | (tmp2.value.as[UInt16])}
        tmp.value
      }
      def read_byte(): UInt8 = {
        val tmp = Reg[UInt8]
        Pipe{tmp := jpg_sram.deq()}
        tmp.value
      }

      def read_first(): UInt8 = {
        val c1 = Reg[UInt8]
        val c2 = Reg[UInt8]
        Pipe{c1 := read_byte()}
        Pipe{c2 := read_byte()}
        if (c1.value != 255.to[UInt8] || c2.value != M_SOI.to[UInt8]) {
          println("Not Jpeg File!")
          errors :+= 1
        }
        c2.value
      }

      val unread_marker = Reg[UInt8](0)

      def next_marker(): UInt8 = {
        val potential_marker = Reg[UInt8](0)
        potential_marker.reset
        FSM(0)(whilst => whilst != -1.to[Int]){whilst =>
          if (read_byte() == 255.to[UInt8]) {
            potential_marker := read_byte()
          }
        }{whilst => mux(potential_marker.value != 0.to[UInt8] || jpg_sram.isEmpty, -1, whilst)}
        potential_marker.value
      }

      def get_dqt(): Unit = {Sequential{
        val length = Reg[UInt16](0)
        Pipe{length := read_word() - 2.to[UInt16]}
        println("lenght " + length.value)
        // if (length != 65) quit()
        FSM(0){iter => iter != -1}{ iter =>
          val tmp = read_byte()
          val prec = tmp >> 4
          val num = (tmp & 0x0f).as[Int]

          Foreach(DCTSIZE2 by 1){ i => 
            val tmp = Reg[UInt16](0)
            Pipe{
              if (prec == 1.to[UInt8]) {
                tmp := read_word()
              } else {
                tmp := read_byte().as[UInt16]
              }
            }
            // println("quantvl tbl " + {num+1} + "," + izigzag_index(i) + " = " + tmp.value)
            // Note CHStone gets a pointer for the last element in row "num", effectively storing into row "num+1"
            p_jinfo_quant_tbl_quantval(num+1, izigzag_index(i)) = tmp.value
          }
          length := length - (DCTSIZE2 + 1).to[UInt16] - mux(prec == 1.to[UInt8], DCTSIZE2.to[UInt16], 0.to[UInt16])
        }{iter => mux(length == 0.to[UInt16], -1, iter + 1)}

        // Foreach(4 by 1, 64 by 1){(i,j) => println(" " + i + "," + izigzag_index(j) + " = " + p_jinfo_quant_tbl_quantval(i,izigzag_index(j)))}

      }}

      def get_sof(): Unit = { Sequential{
        val length = Reg[UInt16](0)
        length := read_word()
        val p_jinfo_data_precision = read_byte()
        p_jinfo_image_height := read_word()
        p_jinfo_image_width := read_word()
        p_jinfo_num_components := read_byte()
        println(" " + length.value + " " + p_jinfo_data_precision + " " + p_jinfo_image_height + " " + p_jinfo_image_width + " " + p_jinfo_num_components)

        // if (length.value != out_length_get_sof || p_jinfo_data_precision != out_data_precision_get_sof ...) {error} 
        Pipe{length :-= 8} // Why the hell do we do this??

        Sequential.Foreach(NUM_COMPONENT by 1) { i => 
          val p_comp_info_index = i.as[UInt8]
          val p_comp_info_id = read_byte()
          val c = read_byte()
          val p_comp_info_h_samp_factor = (c >> 4) & 15
          val p_comp_info_v_samp_factor = (c) & 15
          val p_comp_info_quant_tbl_no = read_byte()

          // Some more checks here

          println("writing " + p_comp_info_index + " " + p_comp_info_id + " " + p_comp_info_h_samp_factor + " " + p_comp_info_v_samp_factor + " " + p_comp_info_quant_tbl_no)
          component(i) = comp_struct(p_comp_info_index, p_comp_info_id, p_comp_info_h_samp_factor, p_comp_info_v_samp_factor, p_comp_info_quant_tbl_no)
        }

        p_jinfo_smp_fact := mux(component(0).h_samp_factor == 2.to[UInt8], 2.to[U2], 0.to[U2])
      }}


      def get_sos(): Unit = {Sequential{
        val length = Reg[UInt16](0)
        length := read_word()
        val num_comp = read_byte()

        println(" length " + length.value)

          Sequential.Foreach(0 until num_comp.to[I32] by 1) { i =>
          val cc = read_byte ();
          val c = read_byte ();

            Foreach(0 until p_jinfo_num_components.value.to[I32] by 1) { ci =>
            val dc = (c >> 4) & 15
            val ac = c & 15
            component_acdc(ci) = comp_acdc(ci.to[UInt8], dc, ac)
          }

        }

        // pluck off 3 bytes for fun
        Foreach(3 by 1) { _ => read_byte()}
        jpeg_data_start := 5207 - jpg_sram.numel
      }}

      def get_dht(): Unit = {Sequential{
        val length = Reg[UInt16](0)
        Pipe{length := read_word() - 2}
        println(" length " + length.value)
        // if (length != 65) quit()
        FSM(0)(whilst => whilst != -1) { whilst =>
          val index = Reg[UInt8](0)
          Pipe{index := read_byte()}
          val store_dc = Reg[Boolean](false)

          if ((index.value & 0x10) == 0.to[UInt8]) {
            store_dc := true
          } else {
            store_dc := false
            index :-= 0x10
          }

          val count = Reg[Int](0)
          count.reset
          Sequential.Foreach(1 until 17 by 1){ i => 
            val tmp = Reg[UInt8](0)
            Pipe{tmp := read_byte()}
            if (store_dc.value) {
                p_jinfo_dc_xhuff_tbl_bits(index.value.to[Int], i.to[I32]) = tmp
            } else {
                p_jinfo_ac_xhuff_tbl_bits(index.value.to[Int], i.to[I32]) = tmp
            }
            // println("p_jinfo_dc_xhuff_tbl_bits write " + index.value.to[Int] + "," + i + " = " + tmp.value)
            Pipe{count := count + tmp.value.to[Int]}
          }

          println(" dht count " + count.value)

          length :-= 17
          Sequential.Foreach(0 until count by 1) { i => 
            if (store_dc.value) {
                p_jinfo_dc_xhuff_tbl_huffval(index.value.to[Int], i.to[I32]) = read_byte()
            } else {
                p_jinfo_ac_xhuff_tbl_huffval(index.value.to[Int], i.to[I32]) = read_byte()
            }
            
          }

          Pipe{length := length - count.value.to[UInt16]}

        }{whilst => mux(length > 16.to[UInt16], whilst, -1)}


      }}

      // START DESIGN

      // Init structures
      Foreach(4 by 1, DCTSIZE2 by 1){(i,j) => p_jinfo_quant_tbl_quantval(i,j) = 0}
      Foreach(NUM_HUFF_TBLS by 1, 36 by 1){(i,j) => p_jinfo_dc_xhuff_tbl_bits(i,j) = 0}
      Foreach(NUM_HUFF_TBLS by 1, 257 by 1){(i,j) => p_jinfo_dc_xhuff_tbl_huffval(i,j) = 0}
      Foreach(NUM_HUFF_TBLS by 1, 36 by 1){(i,j) => p_jinfo_ac_xhuff_tbl_bits(i,j) = 0}
      Foreach(NUM_HUFF_TBLS by 1, 257 by 1){(i,j) => p_jinfo_ac_xhuff_tbl_huffval(i,j) = 0}

      // Load values
      jpg_sram load jpg_dram(0::5207) // TODO: Tile this

      // Read markers
      FSM(0)(sow_SOI => sow_SOI != -1){sow_SOI =>
        if (sow_SOI == 0.to[Int]) {
          Pipe{unread_marker := read_first()}
        } else {
          unread_marker := next_marker()
        }

        println("new marker is " + unread_marker)

        if (unread_marker.value == M_SOI.to[UInt8]) {
          println("M_SOI")
          // Continue normally
        } else if (unread_marker.value == M_SOF0.to[UInt8]) {
          println("M_SOF0")
          get_sof()
        } else if (unread_marker.value == M_DQT.to[UInt8]) {
          println("M_DQT")
          get_dqt()
        } else if (unread_marker.value == M_DHT.to[UInt8]) {
          println("M_DHT")
          get_dht()          
        } else if (unread_marker.value == M_SOS.to[UInt8]) {
          println("M_SOS")
          get_sos()
        } else if (unread_marker.value == M_EOI.to[UInt8]) {
          println("M_EOI")
          eoi_found := true
        } else {
          // Do nothing (probably 0xe0)
        }
      }{sow_SOI => mux(eoi_found, -1, 1)}
    
      // Store intermediate results
      val comp_ac = SRAM[UInt8](NUM_COMPONENT)
      smp_fact := p_jinfo_smp_fact
      quant_tbl_quantval store p_jinfo_quant_tbl_quantval
      dc_xhuff_tbl_bits store p_jinfo_dc_xhuff_tbl_bits
      dc_xhuff_tbl_huffval store p_jinfo_dc_xhuff_tbl_huffval
      ac_xhuff_tbl_bits store p_jinfo_ac_xhuff_tbl_bits
      ac_xhuff_tbl_huffval store p_jinfo_ac_xhuff_tbl_huffval
      errors_found := errors
      component_ac_0 := component_acdc(0).dc
      component_dc_0 := component_acdc(0).ac
      component_ac_1 := component_acdc(1).dc
      component_dc_1 := component_acdc(1).ac
      component_ac_2 := component_acdc(2).dc
      component_dc_2 := component_acdc(2).ac

    }

    // Extract results
    val smp_fact_result = getArg(smp_fact)
    val errors_result = getArg(errors_found)
    val jpeg_data_start_result = getArg(jpeg_data_start)
    val quant_tbl_quantval_result = getMatrix(quant_tbl_quantval)
    val dc_xhuff_tbl_bits_result_algnd = getMatrix(dc_xhuff_tbl_bits)
    val dc_xhuff_tbl_huffval_result_algnd = getMatrix(dc_xhuff_tbl_huffval)
    val ac_xhuff_tbl_bits_result_algnd = getMatrix(ac_xhuff_tbl_bits)
    val ac_xhuff_tbl_huffval_result_algnd = getMatrix(ac_xhuff_tbl_huffval)
    val component_ac_0_result = getArg(component_ac_0)
    val component_dc_0_result = getArg(component_dc_0)
    val component_ac_1_result = getArg(component_ac_1)
    val component_dc_1_result = getArg(component_dc_1)
    val component_ac_2_result = getArg(component_ac_2)
    val component_dc_2_result = getArg(component_dc_2)

    // Reshape results
    val dc_xhuff_tbl_bits_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => dc_xhuff_tbl_bits_result_algnd(i,j)}
    val dc_xhuff_tbl_huffval_result = (0::NUM_HUFF_TBLS, 0::257){(i,j) => dc_xhuff_tbl_huffval_result_algnd(i,j)}
    val ac_xhuff_tbl_bits_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => ac_xhuff_tbl_bits_result_algnd(i,j)}
    val ac_xhuff_tbl_huffval_result = (0::NUM_HUFF_TBLS, 0::257){(i,j) => ac_xhuff_tbl_huffval_result_algnd(i,j)}

    // Get gold checks
    val jpeg_data_start_gold = 623
    val smp_fact_gold = 2
    val quant_tbl_quantval_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_tbl_quantval.csv", " ", "\n")
    val dc_xhuff_tbl_bits_gold = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_bits.csv", " ", "\n")
    val dc_xhuff_tbl_huffval_gold = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_huffval.csv", " ", "\n")
    val ac_xhuff_tbl_bits_gold = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_bits.csv", " ", "\n")
    val ac_xhuff_tbl_huffval_gold = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_huffval.csv", " ", "\n")
    // val component_acdc_gold = loadCSV1D[comp_struct](s"$DATA/machsuite/jpeg_acdc.csv", "\n")

    // Do checks
    println("\nRESULTS: ")
    println("Found " + errors_result + " errors")
    println("smp_fact = " + smp_fact_result)
    println("jpeg_data_start = " + jpeg_data_start_result)
    println("component_dc = " + component_dc_0_result + " " + component_dc_1_result + " " + component_dc_2_result)
    println("component_ac = " + component_ac_0_result + " " + component_ac_1_result + " " + component_ac_2_result)
    printMatrix(quant_tbl_quantval_result, "tbl_quantval")
    printMatrix(dc_xhuff_tbl_bits_result, "dc_xhuff_bits")
    printMatrix(dc_xhuff_tbl_huffval_result, "dc_xhuff_huffval")
    printMatrix(ac_xhuff_tbl_bits_result, "ac_xhuff_bits")
    printMatrix(ac_xhuff_tbl_huffval_result, "ac_xhuff_huffval")

    val cksum_components = component_dc_0 == 1.to[UInt8] && component_dc_1 == 1.to[UInt8] && component_dc_2 == 1.to[UInt8] && component_ac_0 == 1.to[UInt8] && component_ac_1 == 1.to[UInt8] && component_ac_2 == 1.to[UInt8]
    val cksum_data_start = jpeg_data_start_result == jpeg_data_start_gold
    val cksum_smp_fact = smp_fact_result == smp_fact_gold.to[UInt2]
    val cksum_tbl_quantval = quant_tbl_quantval_result.zip(quant_tbl_quantval_gold){_==_}.reduce{_&&_}
    val cksum_dc_xhuff_tbl_bits = dc_xhuff_tbl_bits_result.zip(dc_xhuff_tbl_bits_gold){_==_}.reduce{_&&_}
    val cksum_dc_xhuff_tbl_huffval = dc_xhuff_tbl_huffval_result.zip(dc_xhuff_tbl_huffval_gold){_==_}.reduce{_&&_}
    val cksum_ac_xhuff_tbl_bits = ac_xhuff_tbl_bits_result.zip(ac_xhuff_tbl_bits_gold){_==_}.reduce{_&&_}
    val cksum_ac_xhuff_tbl_huffval = ac_xhuff_tbl_huffval_result.zip(ac_xhuff_tbl_huffval_gold){_==_}.reduce{_&&_}
    println("cksums: " + cksum_smp_fact + " " + cksum_tbl_quantval + " " + cksum_dc_xhuff_tbl_bits + " " + cksum_dc_xhuff_tbl_huffval + " " + cksum_ac_xhuff_tbl_bits + " " + cksum_ac_xhuff_tbl_huffval)
    val cksum = cksum_data_start && cksum_tbl_quantval && cksum_smp_fact && cksum_dc_xhuff_tbl_bits && cksum_dc_xhuff_tbl_huffval && cksum_ac_xhuff_tbl_bits && cksum_ac_xhuff_tbl_huffval && (errors_result == 0)
    println("cksums: " + cksum_data_start + " " + cksum_tbl_quantval + " " + cksum_smp_fact + " " + cksum_dc_xhuff_tbl_bits + " " + cksum_dc_xhuff_tbl_huffval + " " + cksum_ac_xhuff_tbl_bits + " " + cksum_ac_xhuff_tbl_huffval + " " + (errors_result == 0))
    println("PASS: " + cksum + " (JPEG_Markers)")
    assert(cksum)
  }
}

@test class JPEG_Decompress extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type UInt8 = FixPt[FALSE, _8, _0]
  type UInt2 = FixPt[FALSE, _2, _0]
  type UInt16 = FixPt[FALSE, _16, _0]
  type UInt = FixPt[FALSE, _32, _0]
  @struct case class comp_struct(index: UInt8,
                                 id: UInt8,
                                 h_samp_factor: UInt8,
                                 v_samp_factor: UInt8,
                                 quant_tbl_no: UInt8
                                )
  @struct case class comp_acdc(index: UInt8,
                               dc: UInt8,
                               ac: UInt8
                              )


  def main(args: Array[String]): Unit = {

    val M_SOI = 216 // Start of image
    val M_SOF0 = 192 // Baseline DCT ( Huffman ) 
    val M_SOS = 218 // Start of Scan ( Head of Compressed Data )
    val M_DHT = 196
    val M_DQT = 219
    val M_EOI = 217
    val DCTSIZE2 = 64
    val NUM_COMPONENT = 3
    val NUM_HUFF_TBLS = 2
    val RGB_NUM = 3
    val SF4_1_1 = 2
    val SF1_1_1 = 0

    // Validity check values
    val out_length_get_sof = 17
    val out_data_precision_get_sof = 8
    val out_p_jinfo_image_height_get_sof = 59
    val out_p_jinfo_image_width_get_sof = 90
    val out_p_jinfo_num_components_get_sof = 3

    val jpg_data = loadCSV1D[UInt8](s"$DATA/machsuite/jpeg_input.csv", ",")
    val numel = jpg_data.length
    assert(!(jpg_data(0) != 255.to[UInt8] || jpg_data(1) != M_SOI.to[UInt8]), "Not a jpeg file!")

    val jpg_size = ArgIn[Int]
    setArg(jpg_size, numel)
    // val jpg_dram = DRAM[UInt8](jpg_size)
    // setMem(jpg_dram, jpg_data)

    // Get temp results
    val dc_xhuff_tbl_bits_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_bits.csv", " ", "\n")
    val dc_xhuff_tbl_huffval_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_huffval.csv", " ", "\n")
    val ac_xhuff_tbl_bits_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_bits.csv", " ", "\n")
    val ac_xhuff_tbl_huffval_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_huffval.csv", " ", "\n")

    val smp_fact = ArgIn[UInt2]
    val dc_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS,36) // 36)
    val dc_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS,257) // 257)
    val ac_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS,36) // 36)
    val ac_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS,257) // 257)


    setArg(smp_fact, 2.to[UInt2])
    setMem(dc_xhuff_tbl_bits, dc_xhuff_tbl_bits_data)
    setMem(dc_xhuff_tbl_huffval, dc_xhuff_tbl_huffval_data)
    setMem(ac_xhuff_tbl_bits, ac_xhuff_tbl_bits_data)
    setMem(ac_xhuff_tbl_huffval, ac_xhuff_tbl_huffval_data)

    // Result structures
    val dc_dhuff_tbl_ml_0 = ArgOut[UInt16]
    val dc_dhuff_tbl_ml_1 = ArgOut[UInt16]
    val dc_dhuff_tbl_maxcode = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
    val dc_dhuff_tbl_mincode = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
    val dc_dhuff_tbl_valptr = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
    val ac_dhuff_tbl_ml_0 = ArgOut[UInt16]
    val ac_dhuff_tbl_ml_1 = ArgOut[UInt16]
    val ac_dhuff_tbl_maxcode = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
    val ac_dhuff_tbl_mincode = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
    val ac_dhuff_tbl_valptr = DRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)

    Accel{
      // val jpg_sram = FIFO[UInt8](5207)

      val izigzag_index = LUT[Int](64)( 0.to[Int], 1.to[Int], 8.to[Int], 16.to[Int], 9.to[Int], 2.to[Int], 3.to[Int], 10.to[Int],
                                    17.to[Int], 24.to[Int], 32.to[Int], 25.to[Int], 18.to[Int], 11.to[Int], 4.to[Int], 5.to[Int],
                                    12.to[Int], 19.to[Int], 26.to[Int], 33.to[Int], 40.to[Int], 48.to[Int], 41.to[Int], 34.to[Int],
                                    27.to[Int], 20.to[Int], 13.to[Int], 6.to[Int], 7.to[Int], 14.to[Int], 21.to[Int], 28.to[Int],
                                    35.to[Int], 42.to[Int], 49.to[Int], 56.to[Int], 57.to[Int], 50.to[Int], 43.to[Int], 36.to[Int],
                                    29.to[Int], 22.to[Int], 15.to[Int], 23.to[Int], 30.to[Int], 37.to[Int], 44.to[Int], 51.to[Int],
                                    58.to[Int], 59.to[Int], 52.to[Int], 45.to[Int], 38.to[Int], 31.to[Int], 39.to[Int], 46.to[Int],
                                    53.to[Int], 60.to[Int], 61.to[Int], 54.to[Int], 47.to[Int], 55.to[Int], 62.to[Int], 63.to[Int]
                                  )

      val scan_ptr = Reg[Int](1)

      val p_jinfo_dc_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 36) // 36)
      val p_jinfo_dc_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 256) // 256)
      val p_jinfo_dc_dhuff_tbl_ml = SRAM[UInt16](NUM_HUFF_TBLS)
      val p_jinfo_dc_dhuff_tbl_maxcode = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_dc_dhuff_tbl_mincode = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_dc_dhuff_tbl_valptr = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_ac_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 36) // 36)
      val p_jinfo_ac_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 256) // 256)
      val p_jinfo_ac_dhuff_tbl_ml = SRAM[UInt16](NUM_HUFF_TBLS)
      val p_jinfo_ac_dhuff_tbl_maxcode = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_ac_dhuff_tbl_mincode = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)
      val p_jinfo_ac_dhuff_tbl_valptr = SRAM[UInt16](NUM_HUFF_TBLS, 64) // 36)

      val out_data_comp_vpos = SRAM[UInt16](RGB_NUM)
      val out_data_comp_hpos = SRAM[UInt16](RGB_NUM)

      val p_jinfo_num_components = Reg[UInt8](0)
      val p_jinfo_image_height = Reg[UInt16](0)
      val p_jinfo_image_width = Reg[UInt16](0)

      def huff_make_dhuff_tb(idx: Int, p_xhtbl_bits: SRAM2[UInt8], p_dhtbl_ml_mem: SRAM1[UInt16], p_dhtbl_maxcode: SRAM2[UInt16], p_dhtbl_mincode: SRAM2[UInt16], p_dhtbl_valptr: SRAM2[UInt16]): Unit = {
        val huffsize = SRAM[UInt16](257)
        val huffcode = SRAM[UInt16](257)
        val p = Reg[Int](0)
        Sequential.Foreach(1 until 17 by 1) { i => 
          println(" this j_max is " + {p_xhtbl_bits(idx, i) + 1.to[UInt8]} + " from " + idx + " " + i)
          val j_max = (p_xhtbl_bits(idx, i) + 1.to[UInt8]).as[Int]
          Sequential.Foreach(1 until j_max by 1) { j => 
            Pipe{println("writing " + i + " to huffsize " + p.value)}
            Pipe{huffsize(p) = i.as[UInt16]}
            Pipe{p :+= 1}
          }
        }
        Pipe{huffsize(p) = 0}
        val lastp = p
        val code = Reg[UInt16](0)
        val size = Reg[UInt16](0)
        Pipe{size := huffsize(0)}
        val pp = Reg[Int](0)
        Pipe{ code.reset() }


        FSM(0)(whilst1 => whilst1 != -1) { whilst1 =>
          FSM(0)(whilst2 => whilst2 != -1) { whilst2 =>
            // println("huffocde " + pp + " = " + code )
            Pipe{huffcode(pp) = code.value}
            Pipe{code :+= 1}
            Pipe{pp :+= 1}
            println("  conclude first fsm " + huffsize(pp) + " at " + pp + " == " + size.value + " code " + code.value)
          } {whilst2 => mux((huffsize(pp) == size.value) && (pp < 257), whilst2, -1)}

          // println("at " + pp + " size is " + huffsize(pp))
          val break_cond = huffsize(pp) == 0.to[UInt16]

          // if (!break_cond) { // Issue #160
          FSM(0)(whilst2 => whilst2 != -1) { whilst2 =>
            if (!break_cond) {
              Pipe{code := code << 1}
              Pipe{size :+= 1}
            }
            println("  conclude second fsm " + huffsize(pp) + " at " + pp + " == " + size.value + " " + break_cond + " code "  + code.value)
          } { whilst2 => mux((huffsize(pp) == size.value) || break_cond, -1, whilst2)}
          // }
          println("Conclude outer fsm, " + huffsize(pp) + " at " + pp)
        } {whilst1 => mux((huffsize(pp) == 0.to[UInt16]), -1, whilst1)}

        println("exit fsm!")
        val p_dhtbl_ml = Reg[Int](1)
        val ppp = Reg[Int](0)
        Pipe{p_dhtbl_ml.reset}
        Sequential.Foreach(1 until 17 by 1){l => 
          if (p_xhtbl_bits(idx, l) == 0.to[UInt8]) {
            Pipe{p_dhtbl_maxcode(idx, l) = 65535.to[UInt16]} // Signifies skip
          } else {
            Pipe{p_dhtbl_valptr(idx, l) = ppp.value.as[UInt16]}
            Pipe{p_dhtbl_mincode(idx,l) = huffcode(ppp)}
            Pipe{ppp :+= p_xhtbl_bits(idx,l).as[Int] - 1}
            Pipe{p_dhtbl_maxcode(idx,l) = huffcode(ppp)}
            Pipe{p_dhtbl_ml := l}
            println(" " + p_dhtbl_valptr(idx,l) + " " + p_dhtbl_mincode(idx,l) + " " + ppp + " " + p_dhtbl_maxcode(idx,l) + " " + p_dhtbl_ml)
            Pipe{ppp :+= 1}
          }
        }
        Pipe{p_dhtbl_maxcode(idx, p_dhtbl_ml.value) = p_dhtbl_maxcode(idx, p_dhtbl_ml.value) + 1}
        Pipe{p_dhtbl_ml_mem(idx) = p_dhtbl_ml.value.as[UInt16]}
      }


      // START DESIGN

      // Init structures
      p_jinfo_dc_xhuff_tbl_bits load dc_xhuff_tbl_bits(0::2, 0::36)
      p_jinfo_dc_xhuff_tbl_huffval load dc_xhuff_tbl_huffval(0::2, 0::256)
      p_jinfo_ac_xhuff_tbl_bits load ac_xhuff_tbl_bits(0::2, 0::36)
      p_jinfo_ac_xhuff_tbl_huffval load ac_xhuff_tbl_huffval(0::2, 0::256)
      Foreach(NUM_HUFF_TBLS by 1, 64 by 1) {(i,j) => 
        p_jinfo_dc_dhuff_tbl_maxcode(i,j) = 0
        p_jinfo_dc_dhuff_tbl_mincode(i,j) = 0
        p_jinfo_dc_dhuff_tbl_valptr(i,j) = 0
        p_jinfo_ac_dhuff_tbl_maxcode(i,j) = 0
        p_jinfo_ac_dhuff_tbl_mincode(i,j) = 0
        p_jinfo_ac_dhuff_tbl_valptr(i,j) = 0
      }

      Foreach(0 until 2, 0 until 36){(i,j) => println("mem at " + i + "," + j + " = " + p_jinfo_dc_xhuff_tbl_bits(i,j))}
      // // Load values
      // jpg_sram load jpg_dram(0::5207) // TODO: Tile this

      // Decompress
      val p_jinfo_MCUHeight = Reg[UInt16](0)
      val p_jinfo_MCUWidth = Reg[UInt16](0)
      val p_jinfo_NumMCU = Reg[UInt16](0)

      Pipe{p_jinfo_MCUHeight := (p_jinfo_image_height - 1) / 8 + 1}
      Pipe{p_jinfo_MCUWidth := (p_jinfo_image_width - 1) / 8 + 1}
      Pipe{p_jinfo_NumMCU := p_jinfo_MCUHeight * p_jinfo_MCUWidth}

      huff_make_dhuff_tb(0, p_jinfo_dc_xhuff_tbl_bits, p_jinfo_dc_dhuff_tbl_ml, p_jinfo_dc_dhuff_tbl_maxcode,p_jinfo_dc_dhuff_tbl_mincode,p_jinfo_dc_dhuff_tbl_valptr)
      huff_make_dhuff_tb(1, p_jinfo_dc_xhuff_tbl_bits, p_jinfo_dc_dhuff_tbl_ml, p_jinfo_dc_dhuff_tbl_maxcode,p_jinfo_dc_dhuff_tbl_mincode,p_jinfo_dc_dhuff_tbl_valptr)
      huff_make_dhuff_tb(0, p_jinfo_ac_xhuff_tbl_bits, p_jinfo_ac_dhuff_tbl_ml, p_jinfo_ac_dhuff_tbl_maxcode,p_jinfo_ac_dhuff_tbl_mincode,p_jinfo_ac_dhuff_tbl_valptr)
      huff_make_dhuff_tb(1, p_jinfo_ac_xhuff_tbl_bits, p_jinfo_ac_dhuff_tbl_ml, p_jinfo_ac_dhuff_tbl_maxcode,p_jinfo_ac_dhuff_tbl_mincode,p_jinfo_ac_dhuff_tbl_valptr)

      // Store intermediate results
      dc_dhuff_tbl_ml_0 := p_jinfo_dc_dhuff_tbl_ml(0)
      dc_dhuff_tbl_ml_1 := p_jinfo_dc_dhuff_tbl_ml(1)
      dc_dhuff_tbl_maxcode store p_jinfo_dc_dhuff_tbl_maxcode
      dc_dhuff_tbl_mincode store p_jinfo_dc_dhuff_tbl_mincode
      dc_dhuff_tbl_valptr store p_jinfo_dc_dhuff_tbl_valptr
      ac_dhuff_tbl_ml_0 := p_jinfo_ac_dhuff_tbl_ml(0)
      ac_dhuff_tbl_ml_1 := p_jinfo_ac_dhuff_tbl_ml(1)
      ac_dhuff_tbl_maxcode store p_jinfo_ac_dhuff_tbl_maxcode
      ac_dhuff_tbl_mincode store p_jinfo_ac_dhuff_tbl_mincode
      ac_dhuff_tbl_valptr store p_jinfo_ac_dhuff_tbl_valptr
    }


    // Extract results
    val dc_dhuff_tbl_ml_0_result = getArg(dc_dhuff_tbl_ml_0)
    val dc_dhuff_tbl_ml_1_result = getArg(dc_dhuff_tbl_ml_1)
    val dc_dhuff_tbl_maxcode_result_algnd = getMatrix(dc_dhuff_tbl_maxcode)
    val dc_dhuff_tbl_mincode_result_algnd = getMatrix(dc_dhuff_tbl_mincode)
    val dc_dhuff_tbl_valptr_result_algnd = getMatrix(dc_dhuff_tbl_valptr)
    val ac_dhuff_tbl_ml_0_result = getArg(ac_dhuff_tbl_ml_0)
    val ac_dhuff_tbl_ml_1_result = getArg(ac_dhuff_tbl_ml_1)
    val ac_dhuff_tbl_maxcode_result_algnd = getMatrix(ac_dhuff_tbl_maxcode)
    val ac_dhuff_tbl_mincode_result_algnd = getMatrix(ac_dhuff_tbl_mincode)
    val ac_dhuff_tbl_valptr_result_algnd = getMatrix(ac_dhuff_tbl_valptr)

    // Reshape results
    val dc_dhuff_tbl_maxcode_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => dc_dhuff_tbl_maxcode_result_algnd(i,j)}
    val dc_dhuff_tbl_mincode_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => dc_dhuff_tbl_mincode_result_algnd(i,j)}
    val dc_dhuff_tbl_valptr_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => dc_dhuff_tbl_valptr_result_algnd(i,j)}
    val ac_dhuff_tbl_maxcode_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => ac_dhuff_tbl_maxcode_result_algnd(i,j)}
    val ac_dhuff_tbl_mincode_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => ac_dhuff_tbl_mincode_result_algnd(i,j)}
    val ac_dhuff_tbl_valptr_result = (0::NUM_HUFF_TBLS, 0::36){(i,j) => ac_dhuff_tbl_valptr_result_algnd(i,j)}

    // Get gold checks
    val dc_dhuff_tbl_ml_gold = Array[UInt16](9,11)
    val dc_dhuff_tbl_maxcode_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_maxcode.csv", " ", "\n")
    val dc_dhuff_tbl_mincode_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_mincode.csv", " ", "\n")
    val dc_dhuff_tbl_valptr_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_valptr.csv", " ", "\n")
    val ac_dhuff_tbl_ml_gold = Array[UInt16](16,16)
    val ac_dhuff_tbl_maxcode_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_maxcode.csv", " ", "\n")
    val ac_dhuff_tbl_mincode_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_mincode.csv", " ", "\n")
    val ac_dhuff_tbl_valptr_gold = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_valptr.csv", " ", "\n")

    // Do checks
    println("\nRESULTS: ")
    println("dc_dhuff_tbl_ml_result = " + dc_dhuff_tbl_ml_0_result + " " + dc_dhuff_tbl_ml_1_result)
    printMatrix(dc_dhuff_tbl_maxcode_result, "dc_dhuff_tbl_maxcode_result")
    printMatrix(dc_dhuff_tbl_mincode_result, "dc_dhuff_tbl_mincode_result")
    printMatrix(dc_dhuff_tbl_valptr_result, "dc_dhuff_tbl_valptr_result")
    println("ac_dhuff_tbl_ml_result = " + ac_dhuff_tbl_ml_0_result + " " + ac_dhuff_tbl_ml_1_result)
    printMatrix(ac_dhuff_tbl_maxcode_result, "ac_dhuff_tbl_maxcode_result")
    printMatrix(ac_dhuff_tbl_mincode_result, "ac_dhuff_tbl_mincode_result")
    printMatrix(ac_dhuff_tbl_valptr_result, "ac_dhuff_tbl_valptr_result")


    val cksum_dc_dhuff_tbl_ml = dc_dhuff_tbl_ml_0_result == dc_dhuff_tbl_ml_gold(0) && dc_dhuff_tbl_ml_1_result == dc_dhuff_tbl_ml_gold(1)
    val cksum_dc_dhuff_tbl_maxcode = dc_dhuff_tbl_maxcode_result.zip(dc_dhuff_tbl_maxcode_gold){_==_}.reduce{_&&_}
    val cksum_dc_dhuff_tbl_mincode = dc_dhuff_tbl_mincode_result.zip(dc_dhuff_tbl_mincode_gold){_==_}.reduce{_&&_}
    val cksum_dc_dhuff_tbl_valptr = dc_dhuff_tbl_valptr_result.zip(dc_dhuff_tbl_valptr_gold){_==_}.reduce{_&&_}
    val cksum_ac_dhuff_tbl_ml = ac_dhuff_tbl_ml_0_result == ac_dhuff_tbl_ml_gold(0) && ac_dhuff_tbl_ml_1_result == ac_dhuff_tbl_ml_gold(1)
    val cksum_ac_dhuff_tbl_maxcode = ac_dhuff_tbl_maxcode_result.zip(ac_dhuff_tbl_maxcode_gold){_==_}.reduce{_&&_}
    val cksum_ac_dhuff_tbl_mincode = ac_dhuff_tbl_mincode_result.zip(ac_dhuff_tbl_mincode_gold){_==_}.reduce{_&&_}
    val cksum_ac_dhuff_tbl_valptr  = ac_dhuff_tbl_valptr_result.zip(ac_dhuff_tbl_valptr_gold){_==_}.reduce{_&&_}

    val cksum = cksum_dc_dhuff_tbl_ml && cksum_dc_dhuff_tbl_maxcode && cksum_dc_dhuff_tbl_mincode && cksum_dc_dhuff_tbl_valptr && cksum_ac_dhuff_tbl_ml && cksum_ac_dhuff_tbl_maxcode && cksum_ac_dhuff_tbl_mincode && cksum_ac_dhuff_tbl_valptr
    println("PASS: " + cksum + " (JPEG_Decompress)")
    assert(cksum)
  }
}

@test class JPEG_Decode extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = DISABLED
  type UInt8 = FixPt[FALSE, _8, _0]
  type UInt2 = FixPt[FALSE, _2, _0]
  type UInt16 = FixPt[FALSE, _16, _0]
  type UInt = FixPt[FALSE, _32, _0]
  @struct case class comp_struct(index: UInt8,
                                 id: UInt8,
                                 h_samp_factor: UInt8,
                                 v_samp_factor: UInt8,
                                 quant_tbl_no: UInt8
                                )
  @struct case class comp_acdc(index: UInt8,
                               dc: UInt8,
                               ac: UInt8
                              )


  def main(args: Array[String]): Unit = {

    val M_SOI = 216 // Start of image
    val M_SOF0 = 192 // Baseline DCT ( Huffman ) 
    val M_SOS = 218 // Start of Scan ( Head of Compressed Data )
    val M_DHT = 196
    val M_DQT = 219
    val M_EOI = 217
    val DCTSIZE2 = 64
    val NUM_COMPONENT = 3
    val NUM_HUFF_TBLS = 2
    val RGB_NUM = 3
    val SF4_1_1 = 2
    val SF1_1_1 = 0

    // Validity check values
    val out_length_get_sof = 17
    val out_data_precision_get_sof = 8
    val out_p_jinfo_image_height_get_sof = 59
    val out_p_jinfo_image_width_get_sof = 90
    val out_p_jinfo_num_components_get_sof = 3

    val jpg_data = loadCSV1D[UInt8](s"$DATA/machsuite/jpeg_input.csv", ",")
    val numel = jpg_data.length
    assert(!(jpg_data(0) != 255.to[UInt8] || jpg_data(1) != M_SOI.to[UInt8]), "Not a jpeg file!")

    val jpg_size = ArgIn[Int]
    setArg(jpg_size, numel)
    val jpg_dram = DRAM[UInt8](jpg_size)
    setMem(jpg_dram, jpg_data)

    // From previous accel
    val jpeg_data_start = ArgIn[Int]
    val smp_fact = ArgIn[UInt2]
    val dc_dhuff_tbl_ml_0 = ArgIn[UInt16]
    val dc_dhuff_tbl_ml_1 = ArgIn[UInt16]
    val ac_dhuff_tbl_ml_0 = ArgIn[UInt16]
    val ac_dhuff_tbl_ml_1 = ArgIn[UInt16]
    val component_ac_0 = ArgIn[UInt8]
    val component_dc_0 = ArgIn[UInt8]
    val component_ac_1 = ArgIn[UInt8]
    val component_dc_1 = ArgIn[UInt8]
    val component_ac_2 = ArgIn[UInt8]
    val component_dc_2 = ArgIn[UInt8]

    val dc_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS, 36)
    val dc_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS, 257)
    val dc_dhuff_tbl_maxcode = DRAM[UInt16](NUM_HUFF_TBLS, 36)
    val dc_dhuff_tbl_mincode = DRAM[UInt16](NUM_HUFF_TBLS, 36)
    val dc_dhuff_tbl_valptr = DRAM[UInt16](NUM_HUFF_TBLS, 36)
    val ac_xhuff_tbl_bits = DRAM[UInt8](NUM_HUFF_TBLS, 36)
    val ac_xhuff_tbl_huffval = DRAM[UInt8](NUM_HUFF_TBLS, 257)
    val ac_dhuff_tbl_maxcode = DRAM[UInt16](NUM_HUFF_TBLS, 36)
    val ac_dhuff_tbl_mincode = DRAM[UInt16](NUM_HUFF_TBLS, 36)
    val ac_dhuff_tbl_valptr = DRAM[UInt16](NUM_HUFF_TBLS, 36)

    val dc_xhuff_tbl_bits_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_bits.csv", " ", "\n")
    val dc_xhuff_tbl_huffval_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_dc_xhuff_tbl_huffval.csv", " ", "\n")
    val dc_dhuff_tbl_maxcode_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_maxcode.csv", " ", "\n")
    val dc_dhuff_tbl_mincode_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_mincode.csv", " ", "\n")
    val dc_dhuff_tbl_valptr_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_dc_dhuff_tbl_valptr.csv", " ", "\n")
    val ac_dhuff_tbl_ml_data = Array[UInt16](16,16)
    val ac_xhuff_tbl_bits_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_bits.csv", " ", "\n")
    val ac_xhuff_tbl_huffval_data = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_ac_xhuff_tbl_huffval.csv", " ", "\n")
    val ac_dhuff_tbl_maxcode_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_maxcode.csv", " ", "\n")
    val ac_dhuff_tbl_mincode_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_mincode.csv", " ", "\n")
    val ac_dhuff_tbl_valptr_data = loadCSV2D[UInt16](s"$DATA/machsuite/jpeg_ac_dhuff_tbl_valptr.csv", " ", "\n")

    setMem(dc_xhuff_tbl_bits, dc_xhuff_tbl_bits_data)
    setMem(dc_xhuff_tbl_huffval, dc_xhuff_tbl_huffval_data)
    setMem(dc_dhuff_tbl_maxcode, dc_dhuff_tbl_maxcode_data)
    setMem(dc_dhuff_tbl_mincode, dc_dhuff_tbl_mincode_data)
    setMem(dc_dhuff_tbl_valptr, dc_dhuff_tbl_valptr_data)
    setMem(ac_xhuff_tbl_bits, ac_xhuff_tbl_bits_data)
    setMem(ac_xhuff_tbl_huffval, ac_xhuff_tbl_huffval_data)
    setMem(ac_dhuff_tbl_maxcode, ac_dhuff_tbl_maxcode_data)
    setMem(ac_dhuff_tbl_mincode, ac_dhuff_tbl_mincode_data)
    setMem(ac_dhuff_tbl_valptr, ac_dhuff_tbl_valptr_data)
    setArg(dc_dhuff_tbl_ml_0, 9.to[UInt16])
    setArg(dc_dhuff_tbl_ml_1, 11.to[UInt16])
    setArg(ac_dhuff_tbl_ml_0, 16.to[UInt16])
    setArg(ac_dhuff_tbl_ml_1, 16.to[UInt16])
    setArg(jpeg_data_start, 623)
    setArg(smp_fact, 2.to[UInt2])
    setArg(component_ac_0, 0.to[UInt8])
    setArg(component_dc_0, 0.to[UInt8])
    setArg(component_ac_1, 1.to[UInt8])
    setArg(component_dc_1, 1.to[UInt8])
    setArg(component_ac_2, 1.to[UInt8])
    setArg(component_dc_2, 1.to[UInt8])


    Accel{
      val jpg_sram = FIFO[UInt8](5207)
      jpg_sram load jpg_dram(jpeg_data_start::5207)

      val component = SRAM[comp_struct](NUM_COMPONENT)
      val component_acdc = SRAM[comp_acdc](NUM_COMPONENT)
      Pipe{component_acdc(0) = comp_acdc(0.to[UInt8], component_dc_0, component_ac_0)}
      Pipe{component_acdc(1) = comp_acdc(1.to[UInt8], component_dc_1, component_ac_1)}
      Pipe{component_acdc(2) = comp_acdc(2.to[UInt8], component_dc_2, component_ac_2)}
      val izigzag_index = LUT[Int](64)( 0.to[Int], 1.to[Int], 8.to[Int], 16.to[Int], 9.to[Int], 2.to[Int], 3.to[Int], 10.to[Int],
                                    17.to[Int], 24.to[Int], 32.to[Int], 25.to[Int], 18.to[Int], 11.to[Int], 4.to[Int], 5.to[Int],
                                    12.to[Int], 19.to[Int], 26.to[Int], 33.to[Int], 40.to[Int], 48.to[Int], 41.to[Int], 34.to[Int],
                                    27.to[Int], 20.to[Int], 13.to[Int], 6.to[Int], 7.to[Int], 14.to[Int], 21.to[Int], 28.to[Int],
                                    35.to[Int], 42.to[Int], 49.to[Int], 56.to[Int], 57.to[Int], 50.to[Int], 43.to[Int], 36.to[Int],
                                    29.to[Int], 22.to[Int], 15.to[Int], 23.to[Int], 30.to[Int], 37.to[Int], 44.to[Int], 51.to[Int],
                                    58.to[Int], 59.to[Int], 52.to[Int], 45.to[Int], 38.to[Int], 31.to[Int], 39.to[Int], 46.to[Int],
                                    53.to[Int], 60.to[Int], 61.to[Int], 54.to[Int], 47.to[Int], 55.to[Int], 62.to[Int], 63.to[Int]
                                  )
      val bit_set_mask = LUT[UInt](32)(  /* This is 2^i at ith position */
        0x00000001.to[UInt], 0x00000002.to[UInt], 0x00000004.to[UInt], 0x00000008.to[UInt],
        0x00000010.to[UInt], 0x00000020.to[UInt], 0x00000040.to[UInt], 0x00000080.to[UInt],
        0x00000100.to[UInt], 0x00000200.to[UInt], 0x00000400.to[UInt], 0x00000800.to[UInt],
        0x00001000.to[UInt], 0x00002000.to[UInt], 0x00004000.to[UInt], 0x00008000.to[UInt],
        0x00010000.to[UInt], 0x00020000.to[UInt], 0x00040000.to[UInt], 0x00080000.to[UInt],
        0x00100000.to[UInt], 0x00200000.to[UInt], 0x00400000.to[UInt], 0x00800000.to[UInt],
        0x01000000.to[UInt], 0x02000000.to[UInt], 0x04000000.to[UInt], 0x08000000.to[UInt],
        0x10000000.to[UInt], 0x20000000.to[UInt], 0x40000000.to[UInt], 0x80000000.to[UInt]
      )
      val lmask = LUT[UInt](32)( /* This is 2^{i+1}-1 */
        0x00000001.to[UInt], 0x00000003.to[UInt], 0x00000007.to[UInt], 0x0000000f.to[UInt],
        0x0000001f.to[UInt], 0x0000003f.to[UInt], 0x0000007f.to[UInt], 0x000000ff.to[UInt],
        0x000001ff.to[UInt], 0x000003ff.to[UInt], 0x000007ff.to[UInt], 0x00000fff.to[UInt],
        0x00001fff.to[UInt], 0x00003fff.to[UInt], 0x00007fff.to[UInt], 0x0000ffff.to[UInt],
        0x0001ffff.to[UInt], 0x0003ffff.to[UInt], 0x0007ffff.to[UInt], 0x000fffff.to[UInt],
        0x001fffff.to[UInt], 0x003fffff.to[UInt], 0x007fffff.to[UInt], 0x00ffffff.to[UInt],
        0x01ffffff.to[UInt], 0x03ffffff.to[UInt], 0x07ffffff.to[UInt], 0x0fffffff.to[UInt],
        0x1fffffff.to[UInt], 0x3fffffff.to[UInt], 0x7fffffff.to[UInt], 0xffffffff.to[UInt]
      )
      val read_pos_lut = LUT[UInt8](8)( 
        0x01.to[UInt8], 0x02.to[UInt8],
        0x04.to[UInt8], 0x08.to[UInt8],
        0x10.to[UInt8], 0x20.to[UInt8],
        0x40.to[UInt8], 0x80.to[UInt8]
      )
      val extend_mask = LUT[UInt](20)(
        0xFFFFFFFE.to[UInt], 0xFFFFFFFC.to[UInt], 0xFFFFFFF8.to[UInt], 0xFFFFFFF0.to[UInt], 0xFFFFFFE0.to[UInt], 0xFFFFFFC0.to[UInt],
        0xFFFFFF80.to[UInt], 0xFFFFFF00.to[UInt], 0xFFFFFE00.to[UInt], 0xFFFFFC00.to[UInt], 0xFFFFF800.to[UInt], 0xFFFFF000.to[UInt],
        0xFFFFE000.to[UInt], 0xFFFFC000.to[UInt], 0xFFFF8000.to[UInt], 0xFFFF0000.to[UInt], 0xFFFE0000.to[UInt], 0xFFFC0000.to[UInt],
        0xFFF80000.to[UInt], 0xFFF00000.to[UInt]
      )

      val p_jinfo_quant_tbl_quantval = SRAM[UInt16](4,DCTSIZE2)
      val p_jinfo_dc_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 36)
      val p_jinfo_dc_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 257)
      val p_jinfo_dc_dhuff_tbl_maxcode = SRAM[UInt16](NUM_HUFF_TBLS, 36)
      val p_jinfo_dc_dhuff_tbl_mincode = SRAM[UInt16](NUM_HUFF_TBLS, 36)
      val p_jinfo_dc_dhuff_tbl_valptr = SRAM[UInt16](NUM_HUFF_TBLS, 36)
      val p_jinfo_ac_xhuff_tbl_bits = SRAM[UInt8](NUM_HUFF_TBLS, 36)
      val p_jinfo_ac_xhuff_tbl_huffval = SRAM[UInt8](NUM_HUFF_TBLS, 257)
      val p_jinfo_ac_dhuff_tbl_maxcode = SRAM[UInt16](NUM_HUFF_TBLS, 36)
      val p_jinfo_ac_dhuff_tbl_mincode = SRAM[UInt16](NUM_HUFF_TBLS, 36)
      val p_jinfo_ac_dhuff_tbl_valptr = SRAM[UInt16](NUM_HUFF_TBLS, 36)

      val out_data_comp_vpos = SRAM[UInt16](RGB_NUM)
      val out_data_comp_hpos = SRAM[UInt16](RGB_NUM)

      val p_jinfo_num_components = Reg[UInt8](0)
      val p_jinfo_jpeg_data = Reg[Int](0)
      val p_jinfo_image_height = Reg[UInt16](0)
      val p_jinfo_image_width = Reg[UInt16](0)
      val p_jinfo_MCUHeight = Reg[UInt16](0)
      val p_jinfo_MCUWidth = Reg[UInt16](0)
      val p_jinfo_NumMCU = Reg[UInt16](0)

      // Load in intermediate results
      p_jinfo_dc_dhuff_tbl_maxcode load dc_dhuff_tbl_maxcode
      p_jinfo_dc_xhuff_tbl_bits load dc_xhuff_tbl_bits
      p_jinfo_dc_xhuff_tbl_huffval load dc_xhuff_tbl_huffval
      p_jinfo_dc_dhuff_tbl_mincode load dc_dhuff_tbl_mincode
      p_jinfo_dc_dhuff_tbl_valptr load dc_dhuff_tbl_valptr
      p_jinfo_ac_xhuff_tbl_bits load ac_xhuff_tbl_bits
      p_jinfo_ac_xhuff_tbl_huffval load ac_xhuff_tbl_huffval
      p_jinfo_ac_dhuff_tbl_maxcode load ac_dhuff_tbl_maxcode
      p_jinfo_ac_dhuff_tbl_mincode load ac_dhuff_tbl_mincode
      p_jinfo_ac_dhuff_tbl_valptr load ac_dhuff_tbl_valptr
 
      def read_word(): UInt16 = {
        val tmp1 = Reg[UInt8]
        val tmp2 = Reg[UInt8]
        val tmp = Reg[UInt16]
        Pipe{tmp1 := jpg_sram.deq()}
        Pipe{tmp2 := jpg_sram.deq()}
        Pipe{tmp := (tmp1.value.as[UInt16] << 8) | (tmp2.value.as[UInt16])}
        tmp.value
      }
      def read_byte(): UInt8 = {
        val tmp = Reg[UInt8]
        Pipe{tmp := jpg_sram.deq()}
        tmp.value
      }

      // Go to data start
      val read_position_idx = Reg[Int](-1)
      val current_read_byte = Reg[UInt8](0)
      val CurrentMCU = Reg[UInt16](0)
      val HuffBuff = SRAM[UInt32](NUM_COMPONENT,DCTSIZE2)
      val IDCTBuff = SRAM[UInt16](6,DCTSIZE2)

      def pgetc(): UInt8 = {
        val tmp = read_byte()
        if (tmp == 255.to[UInt8]){
          if (read_byte() != 0.to[UInt8]){
            println("Unanticipated marker detected.")
            0.to[UInt8]
          } else {
            255.to[UInt8]
          }
        } else {
          tmp
        }
        // 0.to[UInt8]
      }
      def buf_getb(): UInt8 = {
        if (read_position_idx.value < 0.to[Int]) {
          current_read_byte := pgetc()
          read_position_idx := 7.to[Int]
        }
          // println("            curbyte " + current_read_byte.value + " and position " + read_pos_lut(read_position_idx.value).to[I32])
        val ret = if ((current_read_byte.value & read_pos_lut(read_position_idx.value)) == 0.to[UInt8]){
                    0.to[UInt8]
                  } else {
                    1.to[UInt8]
                  }
        if (read_position_idx.value == 0.to[Int]){read_position_idx := -1} else {read_position_idx :-= 1}
        // if (ret != 0.to[UInt8]) {read_position := read_position.value >> 1; read_position_idx :-= 1} else 
        ret
      }
      def buf_getv(n: UInt8): UInt8 = {
        val ret = Reg[UInt8](0)
        val done = Reg[Boolean](false)
        done.reset()
        val p = Reg[Int8]
        p := n.as[Int8] - read_position_idx.value.as[Int8] - 1.to[Int8]
        // println("p just got " + p.value + " from " + n + " - " + read_position_idx.value)
        FSM(0)(whilst => whilst != -1.to[Int]){whilst =>
          if (p.value > 0) {
            if (read_position_idx.value > 23.to[Int]) { // Not sure how this is ever possible
              val rv = Reg[UInt8](0)
              Pipe{rv := current_read_byte.value}
                Foreach(p.value.to[I32] by 1){i => rv := rv.value << 1} // Manipulate buffer
              current_read_byte := pgetc() // Change read bytes
              val tmp = Reg[UInt8](0)
              Pipe{tmp := current_read_byte}
                Foreach(8-p.value.to[I32] by 1){i => tmp := tmp.value >> 1}
              Pipe{rv := tmp.value | rv.value}
              read_position_idx := 7 - p.value.to[Int]
              done := true
                ret := rv & lmask(n.to[I32]).as[UInt8]
            }
            if (!done.value) {
              current_read_byte := (current_read_byte << 8) | pgetc()
              read_position_idx :+= 8
              p :-= 8
              // println(" p is now " + p.value)
            }
          }
        }{whilst => mux(p.value <= 0 || done.value, -1, 0)}
        if (p.value == 0.to[Int8] && !done.value) {
          read_position_idx := -1.to[Int]
          done := true
            ret := current_read_byte & lmask(n.to[I32]).as[UInt8]
        }
        if (!done.value) {
          p := -p.value
          println(" read pos is now set to " + read_position_idx.value)
          read_position_idx := p.value.to[Int] - 1
          // println("  setting readposition to " + read_pos_lut(read_position_idx.value))
          ret := current_read_byte
            Foreach(p.value.to[I32] by 1) {i => ret := ret.value >> 1}

            ret := ret & lmask(n.to[I32]).as[UInt8]
        }
        ret.value
      }
        def DecodeHuffman(tbl_no: I32, use_dc: Boolean): UInt8 = {
        val code = Reg[Int16](0)
        code := buf_getb().as[Int16]
        // println("entering fsm, code " + code.value)
        val l = Reg[Int](1)
        Pipe{l.reset}
        val max_decode = Reg[Int16](0)
        Pipe{max_decode.reset}
        // Foreach(5 by 1) {i => println("   maxcode " + i + " is " + p_jinfo_dc_dhuff_tbl_maxcode(tbl_no, i))}
        FSM(0)(whilst => whilst != -1.to[Int]){whilst =>
          val tmp = buf_getb().as[Int16]
          // println("  next getb is " + tmp)
          code := (code << 1) + tmp
          l :+= 1
          max_decode := mux(use_dc, p_jinfo_dc_dhuff_tbl_maxcode(tbl_no, l), p_jinfo_ac_dhuff_tbl_maxcode(tbl_no, l)).as[Int16]
          // println("fsm probe " + l.value + " " + max_decode + " " + code.value)
        }{whilst => mux(code.value > max_decode.value, whilst, -1)}
        // println("exit fsm because " + code.value + " < " + max_decode.value + "\n")
        val ac_dhuff_ml = mux(tbl_no == 0, ac_dhuff_tbl_ml_0.value, ac_dhuff_tbl_ml_1.value)
        val dc_dhuff_ml = mux(tbl_no == 0, dc_dhuff_tbl_ml_0.value, dc_dhuff_tbl_ml_1.value)
        val tbl_ml = mux(use_dc, dc_dhuff_ml, ac_dhuff_ml)
            val error_max_decode = mux(use_dc, p_jinfo_dc_dhuff_tbl_maxcode(tbl_no, tbl_ml.as[I32]).to[Int16], p_jinfo_ac_dhuff_tbl_maxcode(tbl_no, tbl_ml.as[I32]).to[Int16])
        // println(" do huf read on " + code.value + " max " + error_max_decode.as[UInt16])
        if (code.value.as[UInt16] < error_max_decode.as[UInt16]) {
          val ptr = mux(use_dc, p_jinfo_dc_dhuff_tbl_valptr(tbl_no, l), p_jinfo_ac_dhuff_tbl_valptr(tbl_no, l))
          val mincode = mux(use_dc, p_jinfo_dc_dhuff_tbl_mincode(tbl_no, l), p_jinfo_ac_dhuff_tbl_mincode(tbl_no, l))
          val p = ptr + code.value.as[UInt16] - mincode
          // println("returning " + ptr + " + " + code.value + " - " + mincode + " == " + p_jinfo_dc_xhuff_tbl_huffval(tbl_no, p.as[Int]) + " or " + p_jinfo_ac_xhuff_tbl_huffval(tbl_no, p.as[Int]))
          mux(use_dc, p_jinfo_dc_xhuff_tbl_huffval(tbl_no, p.as[Int]), p_jinfo_ac_xhuff_tbl_huffval(tbl_no, p.as[Int]))
        } else {
          println("HUFFMAN READ ERROR")
          0.to[UInt8]
        }
      }

          def DecodeHuffMCU(num_cmp: I32, id2: I32): Unit = {
        val tbl_no = component_acdc(num_cmp).dc
        val s = Reg[UInt8](0)
          Pipe{s := DecodeHuffman(tbl_no.to[I32], true)}
        // println("decode huf returned " + s)
        if (s.value != 0.to[UInt8]){
          val diff = Reg[UInt](0)
          diff := buf_getv(s).to[UInt]
          s :-= 1
            if ((diff.value & bit_set_mask(s.value.to[I32])) == 0.to[UInt]) {
              Pipe{diff := diff | extend_mask(s.value.to[I32])}
            Pipe{diff :+= 1.to[UInt]}
          }

          Pipe{diff :+= HuffBuff(id2,0)}
          Pipe{HuffBuff(id2,0) = diff.value}
        }

        Foreach(1 until DCTSIZE2 by 1){i => HuffBuff(id2,i) = 0}
        val error = 1.to[UInt2]
        val break = 2.to[UInt2]
        val action = Reg[UInt2](0)
        val r = Reg[UInt8](0)
        val k = Reg[I32](1)
        k.reset
        FSM(0)(whilst => whilst != -1){whilst =>
          println("decoding huff on ac @ " + k.value)
            Pipe{r := DecodeHuffman(tbl_no.to[I32], false)}
          // println(" s is from " + r.value + " & 0xf")
          s := r & 0xf.to[UInt8]
          val n = (r.value >> 4) & 0xf.to[UInt8]; /* n = run-length */
          if (s.value != 0.to[UInt8]) {
              k :+= n.to[I32]
            if(k >= DCTSIZE2) { /* JPEG Mistake */
              action := error
            } else {
              HuffBuff(id2,k) = buf_getv (s).to[UInt32]  /* Get s bits */
              println("and it is " + read_position_idx.value + " outside here")
              s :-= 1
                if ((HuffBuff(id2,k) & bit_set_mask(s.value.to[I32])) == 0.to[UInt32]) {
                  HuffBuff(id2,k) = HuffBuff(id2,k) | extend_mask(s.value.to[I32]) + 1
              }
              k :+= 1
            }
          } else if (n == 15.to[UInt8]) {
            k :+= 16
          } else {
            action := break
          }
        } {whilst => mux(action == break || action == error || k >= DCTSIZE2, -1, 0)}
        println(" DONE WITH AC")
      }

        def IZigzagMatrix(QuantBuff: SRAM1[UInt16], id2: I32): Unit = {
        Foreach(DCTSIZE2 by 1) { i => 
          QuantBuff(i) = HuffBuff(id2, izigzag_index(i)).to[UInt16]
        }
      }

          // def IQuantize(matrix: SRAM2[UInt32], qmatrix: SRAM2[UInt16], comp_no: I32, id2: I32): Unit = {
      //   Foreach(DCTSIZE2 by 1) { mptr => 
        //     matrix(id2, mptr) = matrix(id2, mptr) * qmatrix(p_comp_info_quant_tbl_no(comp_no).to[I32], mptr)
      //   }
      // }

            def decode_block(comp_no: I32, id1: I32, id2:I32): Unit = {
        val QuantBuff = SRAM[UInt16](DCTSIZE2)
        DecodeHuffMCU(comp_no, id2)
        IZigzagMatrix(QuantBuff, id2)
        // IQuantize(HuffBuff, QuantBuff, comp_no, id2)
      }

      Foreach(NUM_COMPONENT by 1) { i => HuffBuff(i,0) = 0 }
      Foreach(RGB_NUM by 1) { i => out_data_comp_vpos(i) = 0; out_data_comp_hpos(i) = 0 }
      if (smp_fact.value == SF4_1_1.to[UInt2]) {
        println("Decode 4:1:1")
        FSM(0)(whilst => whilst != 1){ whilst =>
          Sequential.Foreach(4 by 1) { i => 
            decode_block(0, i, 0)
          }

        }{ whilst => mux(CurrentMCU.value < p_jinfo_NumMCU.value, whilst, -1)}
      } else {
        // TODO: Implement this                
        println("Decode 1:1:1")
      }
    }

    val gold_bmp = loadCSV2D[UInt8](s"$DATA/machsuite/jpeg_input.csv", ",", "\n")
    // printMatrix(gold_bmp, "gold")
  }
}


@test class MPEG2 extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = DISABLED

  type UInt8 = FixPt[FALSE, _8, _0]
  type UInt2 = FixPt[FALSE, _2, _0]
  type UInt16 = FixPt[FALSE, _16, _0]
  type UInt = FixPt[FALSE, _32, _0]


  def main(args: Array[String]): Unit = {

    val NUMEL = 2048
    val mvscale = 1
    val MV_FIELD = 0
    val ERROR = 255
    val inRdBfr_data = loadCSV1D[UInt8](s"$DATA/machsuite/mpeg2_inRdBfr.csv", ",")
    val inRdBfr_dram = DRAM[UInt8](NUMEL)
    setMem(inRdBfr_dram, inRdBfr_data)

    val motion_vertical_field_select_dram = DRAM[UInt16](32)
    val PMV_dram = DRAM[UInt16](32)

    Accel{
      val inPMV = LUT[UInt16](2,2,2)(45,  207,
                                     70,  41,

                                     4,   180,
                                     120, 216)
      val inmvfs = LUT[UInt16](2,2)(232, 200, 
                                    32,  240)
      val MVtab0 = LUT[Int](8,2)(ERROR, 0, 
                                 3, 3, 
                                 2, 2, 
                                 2, 2,
                                 1, 1, 
                                 1, 1, 
                                 1, 1, 
                                 1, 1)

      val MVtab1 = LUT[Int](8,2)(ERROR, 0, 
                                 ERROR, 0, 
                                 ERROR, 0, 
                                 7, 6,  
                                 6, 6, 
                                 5, 6, 
                                 4, 5, 
                                 4, 5)

      val MVtab2 = LUT[Int](12,2)(16, 9, 
                                 15, 9, 
                                 14, 9, 
                                 13, 9, 
                                 12, 9, 
                                 11, 9,
                                 10, 8, 
                                 10, 8, 
                                 9, 8, 
                                 9, 8, 
                                 8, 8, 
                                 8, 8)

      val dmvector = RegFile[Int](2)
      val motion_vertical_field_select = RegFile[UInt16](2,2)
      val PMV = RegFile[UInt16](2,2,2)

      val ld_Rdbfr = SRAM[UInt8](2048)
      val ld_Bfr = Reg[UInt](68157440)
      val ld_Incnt = Reg[Int](0)
      val ld_Rdptr = Reg[Int](2048)
      val ld_Rdmax = Reg[Int](2047)
      val motion_vector_count = Reg[Int](1)
      val mv_format = Reg[Int](0)
      val h_r_size = Reg[Int](200)
      val v_r_size = Reg[Int](200)
      val dmv = Reg[Boolean](false)
      val s = Reg[Int](0)

      def Fill_Buffer(): Unit = {

        ld_Rdbfr load inRdBfr_dram(0::2048)
        ld_Rdptr := 0
        // TODO: Some padding crap if file is not aligned
      }
      def Flush_Buffer(n: UInt): Unit = {
        Foreach(n.to[Int] by 1){_ => ld_Bfr := ld_Bfr << 1}

        ld_Incnt := ld_Incnt - n.to[Int]
        val Incnt = ld_Incnt
        // Pipe{println("  in flushbuf, ldbfr " + ld_Bfr.value + " n = " + n + " branching based on " + ld_Incnt.value + " and " + ld_Rdptr.value)}

        if (Incnt <= 24) {
          if (ld_Rdptr < 2044) {
            // println("   flushbranch1")
            Foreach(Incnt until 25 by 8){ i => 
              val tmp = Reg[UInt]
              tmp := ld_Rdbfr(ld_Rdptr).to[UInt]
              Foreach(24-i by 1){j => tmp := tmp << 1}
              ld_Bfr := ld_Bfr | tmp.value
            }
          } else {
            // println("   flushbranch2")
            Foreach(Incnt until 25 by 8){i => 
              if (ld_Rdptr >= 2047) {Fill_Buffer()}
              val tmp = Reg[UInt]
              tmp := ld_Rdbfr(ld_Rdptr).to[UInt]
              ld_Rdptr :+= 1 
              Foreach(24-i by 1){j => tmp := tmp << 1}
              ld_Bfr := ld_Bfr | tmp.value
            }
          }
          ld_Incnt := (8-Incnt.value) + 24
        } else {}
      }
      def InitializeBuffer(): Unit = {
        // Regs all set to initial value to begin with
        Flush_Buffer(0.to[UInt])
      }
      def Show_Bits(n: UInt): UInt = {
        val tmp = Reg[UInt]
        tmp := ld_Bfr.value
        Foreach(((32 - n)%32).to[Int] by 1){_ => tmp := tmp >> 1}
        // Pipe{println("showed " + n+  " bits as " + tmp.value)}
        tmp.value
      }
      def Get_Bits(n: UInt): UInt = {
        val bits = Show_Bits(n)
        Flush_Buffer(n.to[UInt])
        bits
      }
      def Get_motion_code(): Int = {
        if (Get_Bits(1) != 0.to[UInt]) {
          0.to[Int]
        } else {
          val code = Reg[Int](0)
          code := Show_Bits(9).to[Int]
          // println("showed bits " + code.value)
          if (code.value >= 64) {
            code := code.value >> 6
            // println("branch1 " + code.value)
            Flush_Buffer(MVtab0(code.value,1).to[UInt])
            val tmp = Get_Bits(1)
            val read = MVtab0(code.value,0)
            if (tmp == 1.to[UInt]) { -read } else { read }
          } else if (code.value >= 24) {
            code := code.value >> 3
            // println("branch2 " + code.value)
            Flush_Buffer(MVtab1(code.value,1).to[UInt])
            val tmp = Get_Bits(1)
            val read = MVtab1(code.value,0)
            // println("mvtab1 is " + read + " tmp is " + tmp)
            if (tmp == 1.to[UInt]) { -read } else { read }
          } else {
            code :-= 12
            if (code < 0) {
              0.to[Int]
            } else {
              Flush_Buffer (MVtab2(code.value,0).to[UInt])
              // println("branch3 " + code.value)
              val tmp = Get_Bits(1)
              val read = MVtab2(code.value,1)
              if (tmp == 1.to[UInt]) { -read } else { read }
            }
          }

        }

      }

      def decode_motion_vector(pmv_id0: Int, pred_id: Int, motion_code: Int, motion_residual: UInt, r_size: Reg[Int], full_pel_vector: Boolean): Unit = {
        r_size := r_size.value % 32
        val lim = Reg[Int](16)
        Foreach(r_size by 1) { _ => lim := lim << 1 }
        val vec = Reg[Int](0)
        val tmp = PMV(pmv_id0, s.value, pred_id)
        vec := mux(full_pel_vector,  tmp >> 1, tmp).to[Int]
        // Pipe{println("motion code " + motion_code + ", vec " + vec.value + ", lim " + lim.value)}
        if (motion_code > 0.to[Int]) {
          val tmp = Reg[Int](0)
          tmp := motion_code - 1
          Foreach(r_size by 1){_ => tmp := tmp.value << 1}
          vec := vec + (tmp.value) + motion_residual.to[Int] + 1
          // Pipe{println("  now vec " + vec.value + " .. + " + tmp.value + " " + motion_residual)}
          if (vec.value >= lim.value.to[Int]) {vec :=  vec.value - (lim.value + lim.value)}
        } else if (motion_code < 0.to[Int]) {
          val tmp = Reg[Int](0)
          tmp := -motion_code - 1
          Foreach(r_size by 1){_ => tmp << 1}
          vec := vec - tmp.value + motion_residual.to[Int] + 1;
          if (vec.value < -(lim.value.to[Int])) {vec := vec.value + lim.value + lim.value}
        }
        // println("new pmv at " + pmv_id0 + "," + s.value + "," + pred_id + " = " + mux(full_pel_vector, vec.value << 1, vec.value))
        PMV(pmv_id0, s.value, pred_id) = mux(full_pel_vector, vec.value << 1, vec.value).to[UInt16]
      }

      def Get_dmvector(): Int = {
        if (Get_Bits(1) != 0.to[UInt]) {
          mux(Get_Bits (1) != 0.to[UInt], -1.to[Int], 1.to[Int])
        } else {
          0.to[Int]
        }
      }
      def motion_vector(full_pel_vector: Boolean, pmv_id0: Int): Unit = {
        // Get horizontal
        val h_motion_code = Get_motion_code()
        val h_motion_residual = if (h_r_size.value != 0.to[Int] && h_motion_code != 0.to[Int]) {Get_Bits(h_r_size.value.to[UInt])} else 0.to[UInt]
        // println(" motion code " + h_motion_code + ", residual " + h_motion_residual)
        decode_motion_vector(pmv_id0, 0.to[Int], h_motion_code, h_motion_residual, h_r_size, full_pel_vector)        
        if (dmv.value) { dmvector(0) = Get_dmvector () }

        // Get vertical
        val v_motion_code = Get_motion_code ();
        val v_motion_residual = if (v_r_size.value != 0.to[Int] && v_motion_code != 0.to[Int]) {Get_Bits(v_r_size.value.to[UInt])} else 0.to[UInt]
        if (mvscale == 1) { Pipe{ PMV(pmv_id0, s.value, 1) = PMV(pmv_id0, s.value, 1) >> 1 /* DIV 2 */}}
        Pipe{decode_motion_vector(pmv_id0, 1.to[Int], v_motion_code, v_motion_residual, v_r_size, full_pel_vector)}
        if (mvscale == 1) { Pipe{ PMV(pmv_id0, s.value, 1) = PMV(pmv_id0, s.value, 1) << 1 /* MUL 2 */}}
        if (dmv) { dmvector(1) = Get_dmvector () }
      }

      def motion_vectors(): Unit = {
        val motion_vertical_field_select = RegFile[UInt](2,2)
        if (motion_vector_count.value == 1) {
          if (mv_format.value == MV_FIELD && !dmv.value) {
            val tmp = Get_Bits(1.to[UInt])
            Foreach(2 by 1) { i => 
              motion_vertical_field_select(i,s.value) = tmp
            }
            motion_vector(false, 0.to[Int])
            /* update other motion vector predictors */
            Pipe{PMV(1, s.value, 0) = PMV(0, s.value, 0)}
            Pipe{PMV(1, s.value, 1) = PMV(0, s.value, 1)}
          }
        } else {
          motion_vertical_field_select(0, s.value) = Get_Bits (1)
          motion_vector (false, 0.to[Int])
          motion_vertical_field_select(1, s.value) = Get_Bits (1)
          motion_vector (false, 1.to[Int])
        }
      }

      Foreach(2 by 1){i =>
        dmvector(i) = 0
        Foreach(2 by 1){j => 
          motion_vertical_field_select(i,j) = inmvfs(i,j)
          Foreach(2 by 1){k => 
            PMV(i,j,k) = inPMV(i,j,k)
          }
        } 
      }

      InitializeBuffer()
      motion_vectors()

      val PMV_aligned = SRAM[UInt16](32)
      val motion_vertical_field_select_aligned = SRAM[UInt16](32)


      Foreach(2 by 1) { i => 
        Foreach(2 by 1) { j => 
          motion_vertical_field_select_aligned(i*2 + j) = motion_vertical_field_select(i,j)
          // println("mot_vert_fs " + i + "," + j + " = " + motion_vertical_field_select(i,j))
          Foreach(2 by 1) { k => 
            PMV_aligned(i*2*2 + j*2 + k) = PMV(i,j,k)
            // println("  pmv " + i + "," + j + "," + k + " = " + PMV(i,j,k))
          }
        }
      }

      PMV_dram store PMV_aligned
      motion_vertical_field_select_dram store motion_vertical_field_select_aligned
    }

    val pmv_gold = Array[UInt16](1566, 206, 70, 41, 1566, 206, 120, 216)
    val mvfs_gold = Array[UInt16](0, 200, 0, 240)
    val mvfs_result = getMem(motion_vertical_field_select_dram)
    val pmv_result = getMem(PMV_dram)

    printArray(pmv_result, "PMV:")
    printArray(mvfs_result, "MVFS:")
    val pmv_cksum = Array.tabulate[Boolean](8){i => pmv_gold(i) === pmv_result(i)}.reduce{_&&_}
    val mvfs_cksum = Array.tabulate[Boolean](4){i => pmv_gold(i) === pmv_result(i)}.reduce{_&&_}
    val cksum = pmv_cksum && mvfs_cksum

    println("PASS: " + cksum + " (MPEG2)")


  }
}

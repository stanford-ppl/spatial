package spatial.tests.apps

import spatial.dsl._

@spatial class BTC extends SpatialTest {
  override def runtimeArgs: Args = {
    "0100000081cd02ab7e569e8bcd9317e2fe99f2de44d49ab2b8851ba4a308000000000000e320b6c2fffc8d750423db8b1eb942ae710e951ed797f7affc8892b0f1fc122bc7f5d74df2b9441a42a14695"
  }

  /*
    According to https://en.bitcoin.it/wiki/Block_hashing_algorithm
    Proof of Work = SHA256(SHA256(HEADER))
  */

  type ULong = FixPt[FALSE, _32, _0]
  type UInt8 = FixPt[FALSE, _8, _0]

  def main(args: Array[String]): Unit = {
    // Setup off-chip data

    val raw_text = args(0)
    val data_text_int = raw_text.map[U8]{c => c}
    val data_text = Array.tabulate(data_text_int.length){i => data_text_int(i).to[UInt8]}
    val len = HostIO[Int]
    setArg(len, data_text.length)
    val text_dram = DRAM[UInt8](1024)
    val hash_dram = DRAM[UInt8](32)//(5)

    println("Hashing: " + raw_text + " (len: " + data_text.length + ")")
    setMem(text_dram, data_text)

    Accel{

      // Init
      val datalen = Reg[Int](0)
      val bitlen = RegFile[ULong](2, List(0.to[ULong],0.to[ULong]))
      val state = RegFile[ULong](8, List(0x6a09e667L.to[ULong],0xbb67ae85L.to[ULong],0x3c6ef372L.to[ULong],0xa54ff53aL.to[ULong],
        0x510e527fL.to[ULong],0x9b05688cL.to[ULong],0x1f83d9abL.to[ULong],0x5be0cd19L.to[ULong])
      )
      val hash = SRAM[UInt8](32)
      val K_LUT = LUT[ULong](64)(
        0x428a2f98L.to[ULong],0x71374491L.to[ULong],0xb5c0fbcfL.to[ULong],0xe9b5dba5L.to[ULong],0x3956c25bL.to[ULong],0x59f111f1L.to[ULong],0x923f82a4L.to[ULong],0xab1c5ed5L.to[ULong],
        0xd807aa98L.to[ULong],0x12835b01L.to[ULong],0x243185beL.to[ULong],0x550c7dc3L.to[ULong],0x72be5d74L.to[ULong],0x80deb1feL.to[ULong],0x9bdc06a7L.to[ULong],0xc19bf174L.to[ULong],
        0xe49b69c1L.to[ULong],0xefbe4786L.to[ULong],0x0fc19dc6L.to[ULong],0x240ca1ccL.to[ULong],0x2de92c6fL.to[ULong],0x4a7484aaL.to[ULong],0x5cb0a9dcL.to[ULong],0x76f988daL.to[ULong],
        0x983e5152L.to[ULong],0xa831c66dL.to[ULong],0xb00327c8L.to[ULong],0xbf597fc7L.to[ULong],0xc6e00bf3L.to[ULong],0xd5a79147L.to[ULong],0x06ca6351L.to[ULong],0x14292967L.to[ULong],
        0x27b70a85L.to[ULong],0x2e1b2138L.to[ULong],0x4d2c6dfcL.to[ULong],0x53380d13L.to[ULong],0x650a7354L.to[ULong],0x766a0abbL.to[ULong],0x81c2c92eL.to[ULong],0x92722c85L.to[ULong],
        0xa2bfe8a1L.to[ULong],0xa81a664bL.to[ULong],0xc24b8b70L.to[ULong],0xc76c51a3L.to[ULong],0xd192e819L.to[ULong],0xd6990624L.to[ULong],0xf40e3585L.to[ULong],0x106aa070L.to[ULong],
        0x19a4c116L.to[ULong],0x1e376c08L.to[ULong],0x2748774cL.to[ULong],0x34b0bcb5L.to[ULong],0x391c0cb3L.to[ULong],0x4ed8aa4aL.to[ULong],0x5b9cca4fL.to[ULong],0x682e6ff3L.to[ULong],
        0x748f82eeL.to[ULong],0x78a5636fL.to[ULong],0x84c87814L.to[ULong],0x8cc70208L.to[ULong],0x90befffaL.to[ULong],0xa4506cebL.to[ULong],0xbef9a3f7L.to[ULong],0xc67178f2L.to[ULong]
      )

      val data = SRAM[UInt8](64)

      def SHFR(x: ULong, y: Int): ULong = {
        val tmp = Reg[ULong](0)
        tmp := x
        Foreach(y by 1){_ => tmp := tmp >> 1}
        tmp.value
      }

      // DBL_INT_ADD treats two unsigned ints a and b as one 64-bit integer and adds c to it
      def DBL_INT_ADD(c:ULong): Unit = {
        if (bitlen(0) > 0xffffffffL.to[ULong] - c) {bitlen(1) = bitlen(1) + 1}
        bitlen(0) = bitlen(0) + c
      }

      def SIG0(x:ULong): ULong = {
        // (ROTRIGHT(x,7) ^ ROTRIGHT(x,18) ^ ((x) >> 3))
        ( x >> 7 | x << (32-7) ) ^ ( x >> 18 | x << (32-18) ) ^ x >> 3
      }

      def SIG1(x:ULong): ULong = {
        // (ROTRIGHT(x,17) ^ ROTRIGHT(x,19) ^ ((x) >> 10))
        ( x >> 17 | x << (32-17) ) ^ ( x >> 19 | x << (32-19) ) ^ x >> 10
      }

      def CH(x:ULong, y:ULong, z:ULong): ULong = {
        // (((x) & (y)) ^ (~(x) & (z)))
        (x & y) ^ (~x & z)
      }

      def MAJ(x:ULong, y:ULong, z:ULong): ULong = {
        // (((x) & (y)) ^ ((x) & (z)) ^ ((y) & (z)))
        (x & y) ^ (x & z) ^ (y & z)
      }

      def EP0(x: ULong): ULong = {
        // (ROTRIGHT(x,2) ^ ROTRIGHT(x,13) ^ ROTRIGHT(x,22))
        ( x >> 2 | x << (32-2) ) ^ ( x >> 13 | x << (32-13) ) ^ ( x >> 22 | x << (32-22) )
      }

      def EP1(x: ULong): ULong = {
        // (ROTRIGHT(x,6) ^ ROTRIGHT(x,11) ^ ROTRIGHT(x,25))
        ( x >> 6 | x << (32-6) ) ^ ( x >> 11 | x << (32-11) ) ^ ( x >> 25 | x << (32-25) )
      }

      def sha_transform(): Unit = {
        val m = SRAM[ULong](64)
        Foreach(0 until 64 by 1){i =>
          if ( i.to[I32] < 16 ) {
            val j = 4*i.to[I32]
            // println(" m(" + i + ") = " + {(data(j).as[ULong] << 24) | (data(j+1).as[ULong] << 16) | (data(j+2).as[ULong] << 8) | (data(j+3).as[ULong])})
            m(i) = (data(j).as[ULong] << 24) | (data(j+1).as[ULong] << 16) | (data(j+2).as[ULong] << 8) | (data(j+3).as[ULong])
          } else {
            // println(" m(" + i + ") = " + SIG1(m(i-2)) + " " + m(i-7) + " " + SIG0(m(i-15)) + " " + m(i-16))
            m(i) = SIG1(m(i-2)) + m(i-7) + SIG0(m(i-15)) + m(i-16)
          }
          // val j = 4*i.to[I32]
          // m(i) = if (i.to[I32] < 16) {(data(j).as[ULong] << 24) | (data(j+1).as[ULong] << 16) | (data(j+2).as[ULong] << 8) | (data(j+3).as[ULong])}
          //        else {SIG1(m(i-2)) + m(i-7) + SIG0(m(i-15)) + m(i-16)}
        }
        val A = Reg[ULong]
        val B = Reg[ULong]
        val C = Reg[ULong]
        val D = Reg[ULong]
        val E = Reg[ULong]
        val F = Reg[ULong]
        val G = Reg[ULong]
        val H = Reg[ULong]

        A := state(0)
        B := state(1)
        C := state(2)
        D := state(3)
        E := state(4)
        F := state(5)
        G := state(6)
        H := state(7)

        Foreach(64 by 1){ i =>
          val tmp1 = H + EP1(E) + CH(E,F,G) + K_LUT(i) + m(i)
          val tmp2 = EP0(A) + MAJ(A,B,C)
          // println(" " + i + " : " + A.value + " " + B.value + " " +
          //   C.value + " " + D.value + " " + E.value + " " + F.value + " " + G.value + " " + H.value)
          // println("    " + H.value + " " + EP1(E) + " " + CH(E,F,G) + " " + K_LUT(i) + " " + m(i))
          H := G; G := F; F := E; E := D + tmp1; D := C; C := B; B := A; A := tmp1 + tmp2
        }

        Foreach(8 by 1 par 8){i =>
          state(i) = state(i) + mux(i.to[I32] == 0, A, mux(i.to[I32] == 1, B, mux(i.to[I32] == 2, C, mux(i.to[I32] == 3, D,
            mux(i.to[I32] == 4, E, mux(i.to[I32] == 5, F, mux(i.to[I32] == 6, G, H)))))))
        }

      }

      def SHA256(): Unit = {
        // Init
        Pipe{
          bitlen.reset
          state.reset
        }

        // Update
        Sequential.Foreach(0 until len.value by 64 par 1) { i =>
          datalen := min(len.value - i, 64)
          // println(" datalen " + datalen.value + " and i " + i + " and len " + len.value)
          data load text_dram(i::i+datalen.value)
          if (datalen.value == 64.to[Int]) {
            // println("doing this " + datalen.value)
            sha_transform()
            DBL_INT_ADD(512);
          }
        }

        // Final
        val pad_stop = if (datalen.value < 56) 56 else 64
        Foreach(datalen until pad_stop by 1){i => data(i) = if (i.to[I32] == datalen) 0x80.to[UInt8] else 0.to[UInt8]}
        if (datalen.value >= 56) {
          sha_transform()
          Foreach(56 by 1){i => data(i) = 0}
        }

        DBL_INT_ADD(datalen.value.to[ULong] * 8.to[ULong])
        Pipe{data(63) = (bitlen(0)).to[UInt8]}
        Pipe{data(62) = (bitlen(0) >> 8).to[UInt8]}
        Pipe{data(61) = (bitlen(0) >> 16).to[UInt8]}
        Pipe{data(60) = (bitlen(0) >> 24).to[UInt8]}
        Pipe{data(59) = (bitlen(1)).to[UInt8]}
        Pipe{data(58) = (bitlen(1) >> 8).to[UInt8]}
        Pipe{data(57) = (bitlen(1) >> 16).to[UInt8]}
        Pipe{data(56) = (bitlen(1) >> 24).to[UInt8]}
        sha_transform()

        // Foreach(8 by 1){i => println(" " + state(i))}

        Sequential.Foreach(4 by 1){ i =>
          hash(i)    = (SHFR(state(0), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+4)  = (SHFR(state(1), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+8)  = (SHFR(state(2), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+12) = (SHFR(state(3), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+16) = (SHFR(state(4), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+20) = (SHFR(state(5), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+24) = (SHFR(state(6), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
          hash(i+28) = (SHFR(state(7), (24-i.to[I32]*8))).bits(7::0).as[UInt8]
        }

      }

      Sequential.Foreach(2 by 1){i =>
        Pipe{SHA256()}
        if (i.to[I32] == 0) {
          text_dram(0::32) store hash
          len := 32
        }
      }

      hash_dram store hash
    }

    val hashed_result = getMem(hash_dram)
    val hashed_gold = Array[UInt8](101.to[UInt8],0.to[UInt8],241.to[UInt8],59.to[UInt8],194.to[UInt8],84.to[UInt8],197.to[UInt8],158.to[UInt8],159.to[UInt8],61.to[UInt8],119.to[UInt8],189.to[UInt8],11.to[UInt8],25.to[UInt8],153.to[UInt8],230.to[UInt8],134.to[UInt8],250.to[UInt8],223.to[UInt8],119.to[UInt8],101.to[UInt8],174.to[UInt8],43.to[UInt8],89.to[UInt8],38.to[UInt8],109.to[UInt8],29.to[UInt8],131.to[UInt8],91.to[UInt8],134.to[UInt8],144.to[UInt8],131.to[UInt8])
    printArray(hashed_gold, "Expected: ")
    printArray(hashed_result, "Got: ")

    val cksum = hashed_gold.zip(hashed_result){_==_}.reduce{_&&_}
    println("PASS: " + cksum + " (BTC)")
    assert(cksum)
  }
}
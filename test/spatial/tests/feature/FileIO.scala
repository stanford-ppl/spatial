package spatial.tests.feature

import spatial.dsl._

@test class BinaryFileTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs

  type Nibble = FixPt[TRUE,_4,_0]
  type UByte = FixPt[FALSE,_8,_0]
  type UShort = FixPt[FALSE,_16,_0]
  type UInt = FixPt[FALSE,_32,_0]

  def main(args: Array[String]): Void = {
    Accel { /* No hardware stuff in this test :( */ }

    val nibbles = Array.tabulate(32){i => i.to[Nibble] }
    val bytes = Array.tabulate(256){i => (i - 128).to[Byte] }
    val ubytes = Array.tabulate(256){i => i.to[UByte] }
    val shorts = Array.tabulate(256){i => ((i - 128)*256).to[Short] }
    val ushorts = Array.tabulate(256){i => (i * 256).to[UShort] }
    val ints = Array.tabulate(256){i => ((i - 128) * 256 * 256).to[Int] }
    val uints = Array.tabulate(256){i => (i * 256).to[UInt] * 256 }

    writeBinary(nibbles, "nibbles.dat")
    writeBinary(bytes, "bytes.dat")
    writeBinary(ubytes, "ubytes.dat")
    writeBinary(shorts, "shorts.dat")
    writeBinary(ushorts, "ushorts.dat")
    writeBinary(ints, "ints.dat")
    writeBinary(uints, "uints.dat")

    val nibblesIn = loadBinary[Nibble]("nibbles.dat")
    val bytesIn = loadBinary[Byte]("bytes.dat")
    val ubytesIn = loadBinary[UByte]("ubytes.dat")
    val shortsIn = loadBinary[Short]("shorts.dat")
    val ushortsIn = loadBinary[UShort]("ushorts.dat")
    val intsIn = loadBinary[Int]("ints.dat")
    val uintsIn = loadBinary[UInt]("uints.dat")

    val nibblesMatch = compare(nibbles, nibblesIn, "Nibbles")
    val bytesMatch = compare(bytes, bytesIn, "Bytes")
    val ubytesMatch = compare(ubytes, ubytesIn, "UBytes")
    val shortsMatch = compare(shorts, shortsIn, "Shorts")
    val ushortsMatch = compare(ushorts, ushortsIn, "UShorts")
    val intsMatch = compare(ints, intsIn, "Ints")
    val uintsMatch = compare(uints, uintsIn, "UInts")

    assert(nibblesMatch && bytesMatch && ubytesMatch && shortsMatch && ushortsMatch && intsMatch && uintsMatch, "One or more tests failed")
  }

  def compare[T:Num](a: Array[T], b: Array[T], name: String): Boolean = {
    val matches = a == b
    if (!matches) {
      println(s"$name: FAIL")
      a.zip(b){(x,y) => pack(x,y) }.foreach{x => if (x._1 != x._2) println("expected: " + x._1 + ", result: " + x._2) else () }
    }
    else {
      println(s"$name: PASS")
    }
    matches
  }
}


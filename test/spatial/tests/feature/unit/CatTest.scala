package spatial.tests.feature.unit

import spatial.dsl._

@spatial class CatTest extends SpatialTest {
  override def runtimeArgs: Args = "3 -7 57 182"

  def main(args: Array[String]): Unit = {
    val b8_1 = ArgIn[I8]
    val b8_2 = ArgIn[I8]
    val b16 = ArgIn[I16]
    val b32 = ArgIn[I32]
    setArg(b8_1, args(0).to[I8])
    setArg(b8_2, args(1).to[I8])
    setArg(b16, args(2).to[I16])
    setArg(b32, args(3).to[I32])

    val cat2 = ArgOut[I16]
    val cat3 = ArgOut[I32]
    val cat4 = ArgOut[I64]
    val cat5 = ArgOut[I8]

    Accel {
      cat2 := cat(b8_1.asBits, b8_2.asBits).as[I16]
      cat3 := cat(b8_1.asBits, b8_2.asBits, b16.asBits).as[I32]
      cat4 := cat(b8_1.asBits, b8_2.asBits, b16.asBits, b32.asBits).as[I64]
      cat5 := cat(b8_1.value.bits(3::0), b8_2.value.bits(3::0)).as[I8]

    }
    println(r"Got  $cat2 $cat3 $cat4 $cat5")
    val gold2 = (args(0).to[I8].as[I16] << 8)  + args(1).to[I8].as[I16]
    val gold3 = (args(0).to[I8].as[I32] << 24) + (args(1).to[I8].as[I32] << 16) +  args(2).to[I16].as[I32]
    val gold4 = (args(0).to[I8].as[I64] << 56) + (args(1).to[I8].as[I64] << 48) +  (args(2).to[I16].as[I64] << 32) + args(3).to[I32].as[I64]
    val gold5 = (args(0).to[I4].as[I8] << 4) + (args(1).to[I4].as[I8])
    println(r"Want $gold2 $gold3 $gold4 $gold5")
    println(r"Result ${gold2 == getArg(cat2)} ${gold3 == getArg(cat3)} ${gold4 == getArg(cat4)} ${gold5 == getArg(cat5)}")
    assert(gold2 == getArg(cat2) && gold3 == getArg(cat3) && gold4 == getArg(cat4) && gold5 == getArg(cat5))
    println("          .__....._             _.....__,")
    println("             .\": o :':         ;': o :\".")
    println("             `. `-' .'.       .'. `-' .'  ")
    println("               `---'             `---' ")
println("")
    println("     _...----...      ...   ...      ...----..._")
    println("  .-'__..-\"\"'----    `.  `\"`  .'    ----'\"\"-..__`-.")
    println(" '.-'   _.--\"\"\"'       `-._.-'       '\"\"\"--._   `-.`")
    println(" '  .-\"'                  :                  `\"-.  `")
    println("   '   `.              _.'\"'._              .'   `")
    println("         `.       ,.-'\"       \"'-.,       .'")
    println("           `.                           .'")
    println("      jgs    `-._                   _.-'")
    println("                 `\"'--...___...--'\"`")
    println("Meow")
  }
}

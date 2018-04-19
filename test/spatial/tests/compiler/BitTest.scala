package spatial.tests.compiler

import spatial.dsl._

import scala.collection.mutable.ArrayBuffer

@test class BitTest extends SpatialTest {
  override def runtimeArgs: Args = NoArgs
  override def backends = DISABLE

  // Returns a random number in [min,max)
  def rand(max: gen.Int, min: gen.Int): gen.Int = scala.util.Random.nextInt(max-min)+min

  def opp(x: Bit, y: Bit, op: gen.Int): Bit = op match {
    case 0 | 1 | 2 => x & y
    case 3 | 4 | 5 => x | y
    case 6 | 7 | 8 => x !== y
    case 9 | 10 | 11 => x === y
    case 12 => !x
    case 13 => !y
  }

  def main(args: Array[String]): Void = {
    Foreach(0 until 32){i =>
      val bits: List[Bit] = List.fill(32){ random[Bit] }
      var layers: ArrayBuffer[List[Bit]] = ArrayBuffer(bits)

      (0 until 64).meta.foreach{i =>
        val layer = List.fill(200){
          val l1 = i //rand(layers.length,0)
          val l2 = i //rand(layers.length,0)
          val p1 = rand(layers(l1).length, 0)
          val p2 = rand(layers(l2).length, 0)
          val op = rand(14,0)
          val x = layers(l1).apply(p1)
          val y = layers(l2).apply(p2)
          opp(x,y,op)
        }
        layers += layer

        println(r"[$i] 1: ${layer(1)}, 3: ${layer(3)}, 5: ${layer(5)}")
      }
    }
  }
}

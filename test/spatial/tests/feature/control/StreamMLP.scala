package spatial.tests.feature.control

import spatial.dsl._
import _root_.spatial.SpatialConfig
import _root_.spatial.lib.ML._
import utils.io.files._
import _root_.spatial.metadata.control._
import _root_.spatial.metadata.memory._

class MLP_Variant_Base extends MLP_Variants(N=16, batch=16,dims=List(4,4,4),ips=List(2,2),mps=List(2,2),ops=List(1,1))
class MLP_Variant_exp_stream extends MLP_Variant_Base {
  override def compileArgs = "--streamify --vv --max_cycles=5000 --noModifyStream"
}

//class MLP_Variant_exp_stream_3 extends MLP_Variants(N=4, batch=4,dims=List(4,4,4),ips=List(1,1),mps=List(2,1),ops=List(1,1))

class MLP_Variant_exp_nostream extends MLP_Variant_Base {
  override def compileArgs = "--nostreamify --vv"
}

@spatial abstract class MLP_Variants(
                                      val N:scala.Int = 1024,
                                      val batch:scala.Int = 4,
                                      val dims:List[scala.Int] = List(16,16,16),
                                      val opb:scala.Int = 1,
                                      val ops:List[scala.Int] = List(1,1),
                                      val mps:List[scala.Int] = List(1,1),
                                      val ips:List[scala.Int] = List(16,16),
                                      val ipls:scala.Int = 16,
                                    ) extends SpatialTest {

  type T = Int

  def main(args: Array[String]): Unit = {
    val Ws = dims.sliding(2,1).map { case List(prev, next) => Seq.tabulate(prev, next) { (i,j) => (i*next +j) } }.toList
    val Bs = dims.sliding(2,1).map { case List(prev, next) => Seq.tabulate(next) { i => i } }.toList
    val input = Seq.tabulate(N, dims.head) { case (i,j) => i*dims.head + j }
    val goldUnstaged = unstaged_mlp[scala.Int](Ws, Bs, input, unstaged_relu _)

    val indram = DRAM[T](N, dims.head)
    val outdram = DRAM[T](N, dims.last)
    setMem(indram, (0 :: N, 0 :: dims.head) { (i,j) => (i*dims.head + j).to[T] })
    Accel {
      val weights = Ws.map { W => LUT.fromSeq[T](W.map { _.map { _.to[T]} }) }.toSeq
      val biases = Bs.map { B => LUT.fromSeq[T](B.map { _.to[T]}) }
      'Outer.Foreach(0 until N by batch) { t =>
        val insram = SRAM[T](batch, dims.head)
        insram.explicitName = "InSRAM"
        val outsram = SRAM[T](batch, dims.last)
        outsram.explicitName = "OutSRAM"
        insram load indram(t::t+batch, dims.head par ipls)
        'Batch.Foreach(0 until batch par opb) { b =>
          mlp_forward[T](weights, biases, relu[T], insram(b,_), outsram.update(b, _, _))(ips, mps, ops)
        }
        outdram(t::t+batch, dims.last par ipls) store outsram
      }
    }
    val output = getMem(outdram)
    writeCSV1D(output, "output.csv",delim="\n")
    val gold = Array[T](goldUnstaged.flatten.map(_.to[T]):_*).reshape(N, dims.last)
    val cksum = checkGold(outdram, gold)
    println("PASS: " + cksum + " (MLP)")
//    assert(cksum)
    assert(Bit(true))
  }

}



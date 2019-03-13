package spatial.tests.dse

import spatial.dsl._

/**
	Scala generator (run this and paste the results in this file):

// Full range:
var used = Set[String]()
(0 until 500).foreach{cfg => 
  val _load = scala.math.abs(scala.util.Random.nextInt % 5)
  val _store = scala.math.abs(scala.util.Random.nextInt % 5)
  val _gated = scala.math.abs(scala.util.Random.nextInt % 5)
  val choose1 = if (_load == 0 && _store == 0 && _gated == 0) scala.math.abs(scala.util.Random.nextInt % 3) else -1
  val load =   if (choose1 == 0) 1 else  _load
  val store =  if (choose1 == 1) 1 else  _store
  val gated =  if (choose1 == 2) 1 else  _gated
  val width = List(16, 512, 1024, 2048, 3120).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val height = List(1, 4, 8, 16, 32).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val bpc = List(32, 64, 128, 256, 512).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val app = s"class Load${load}Store${store}Gated${gated}Dims${height}x${width*32/bpc}P${bpc} extends GenericLoadStore($load,$store,$gated,$height,$width,$bpc)" 
  if (used.contains(app)) print("// ")
  used += app
  println(app)
}


// Optimized for less congestion, shorter loads:
var used = Set[String]()
(0 until 100).foreach{cfg => 
  val _load = scala.math.abs(scala.util.Random.nextInt % 4)
  val _store = scala.math.abs(scala.util.Random.nextInt % 4)
  val _gated = scala.math.abs(scala.util.Random.nextInt % 4)
  val choose1 = if (_load == 0 && _store == 0 && _gated == 0) scala.math.abs(scala.util.Random.nextInt % 3) else -1
  val load =   if (choose1 == 0) 1 else  _load
  val store =  if (choose1 == 1) 1 else  _store
  val gated =  if (choose1 == 2) 1 else  _gated
  val width = List(16, 32, 64, 128, 256).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val height = List(1, 2, 4, 6, 8).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val bpc = List(32, 64, 128, 256, 512).apply(scala.math.abs(scala.util.Random.nextInt % 5))
  val app = s"class Load${load}Store${store}Gated${gated}Dims${height}x${width*32/bpc}P${bpc} extends GenericLoadStore($load,$store,$gated,$height,$width,$bpc)" 
  if (used.contains(app)) print("// ")
  used += app
  println(app)
}

*/
class

@spatial abstract class GenericLoadStore(loads: scala.Int, stores: scala.Int, gated: scala.Int, height: scala.Int, width: scala.Int, bitsPerCycle: scala.Int) extends SpatialTest {
  override def backends: Seq[Backend] = DISABLED

  def main(args: Array[String]): Unit = {
  	val load_drams = List.tabulate(loads){i => DRAM[Int](height,width)}
  	val store_drams = List.tabulate(stores){i => DRAM[Int](height,width)}
  	val gated_store_drams = List.tabulate(gated){i => DRAM[Int](height,width)}
  	val dummy = ArgOut[Int]

  	Accel {
  		val load_srams = List.tabulate(loads){i => SRAM[Int](height,width)}
  		val store_srams = List.tabulate(stores){i => SRAM[Int](height,width)}
  		val gated_store_srams = List.tabulate(gated){i => SRAM[Int](height,width-1)}
  		Parallel{
  			load_srams.zip(load_drams).foreach{case (a,b) => 'LOAD.Pipe{a load b(0::height, 0::width par bitsPerCycle/32)}}
  			store_srams.zip(store_drams).foreach{case (a,b) => 'STORE.Pipe{b store a(0::height, 0::width par bitsPerCycle/32)}}
  			gated_store_srams.zip(gated_store_drams).foreach{case (a,b) => 'GATED.Pipe{b(0::height,0::width-1) store a(0::height, 0::width-1 par bitsPerCycle/32)}}
  		}
  		if (load_srams.size > 0 ) dummy := load_srams.map(_(0,0)).reduceTree{_+_}
  	}

  	println(r"got $dummy")
  	store_drams.foreach{x => printMatrix(getMatrix(x), s"${x.hashCode}")}
  	gated_store_drams.foreach{x => printMatrix(getMatrix(x), s"${x.hashCode}")}
  	assert(Bit(true))
  }
}
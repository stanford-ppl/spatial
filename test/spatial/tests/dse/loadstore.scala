package spatial.tests.dse

import spatial.dsl._

/**
	Scala generator (run this and paste the results in this file):
(1 until 8 by 2).foreach{load => 
  (0 until 6 by 3).foreach{store => 
    (0 until 6 by 3).foreach{gated => 
      (16 until 3000 by 256).foreach{ width => 
        (1 until 16 by 4).foreach{ height => 
          println(s"class Load${load}Store${store}Gated${gated}Dims${height}x${width} extends GenericLoadStore($load,$store,$gated,$height,$width)")
        }
      }
    }
  }
}
*/


@spatial abstract class GenericLoadStore(loads: scala.Int, stores: scala.Int, gated: scala.Int, height: scala.Int, width: scala.Int) extends SpatialTest {
  // override def backends: Seq[Backend] = DISABLED

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
  			load_srams.zip(load_drams).foreach{case (a,b) => 'LOAD.Pipe{a load b}}
  			store_srams.zip(store_drams).foreach{case (a,b) => 'STORE.Pipe{b store a}}
  			gated_store_srams.zip(gated_store_drams).foreach{case (a,b) => 'GATED.Pipe{b(0::height,0::width-1) store a}}
  		}
  		dummy := load_srams.map(_(0,0)).reduceTree{_+_}
  	}

  	println(r"got $dummy")
  	store_drams.foreach{x => printMatrix(getMatrix(x), s"${x.hashCode}")}
  	gated_store_drams.foreach{x => printMatrix(getMatrix(x), s"${x.hashCode}")}
  	assert(Bit(true))
  }
}
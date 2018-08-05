package spatial.tests.feature.banking

import spatial.dsl._

@spatial class Bank2DSimple extends SpatialTest {
  val R = 32; val C = 16
  val P = 1;  val Q = 4

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](R,C)

    Accel {
      val x = SRAM[Int](R,C)

      Foreach(0 until R, 0 until C par Q){(i,j) =>
        x(i,j) = i + j
      }
      dram store x
    }

    val gold = (0::R,0::C){(i,j) => i + j}
    val data = getMatrix(dram)
    printMatrix(data, "data")
    printMatrix(gold, "gold")
    assert(data == gold)
  }
}

@spatial class ComplicatedMuxPort extends SpatialTest {
  val R = 32; val C = 16
  val P = 1;  val Q = 4

  def main(args: Array[String]): Unit = {
    val dram = DRAM[Int](R,C)
    val N = 5

    Accel {
      val x = SRAM[Int](R,C)

      Foreach(N by 1){_ =>
        Sequential.Foreach(0 until R by 2){i =>
          Foreach(0 until C by 1){j => x(i,j) = i + j }
          Foreach(0 until C by 1 par Q){j => x(i+1,j) = i + 1 + j }
        }
        dram store x
      }
      
    }

    val gold = (0::R,0::C){(i,j) => i + j}
    val data = getMatrix(dram)
    printMatrix(data, "data")
    printMatrix(gold, "gold")
    assert(data == gold)
  }

}


@spatial class Bank2DStrange extends SpatialTest {
  override def runtimeArgs: Args = ""

  type T = FixPt[TRUE,_16,_0]
  
  def main(args: Array[String]): Unit = {

    val flat_dram = DRAM[T](32)
    val dim2_dram = DRAM[T](15,15)

    val result_dram = DRAM[T](10,10)

    val flat_data = (0::32){(k) => (k).to[T]}
    setMem(flat_dram, flat_data)

    val dim2_data = (0::15, 0::15){(k,l) => (l + k*15).to[T]}
    setMem(dim2_dram, dim2_data)

    Accel {
    
      val dim2_sram = SRAM[T](15,15)
      dim2_sram load dim2_dram
      
      val tmp = SRAM[T](10, 10)
      val flat_sram = SRAM[T](32)
      flat_sram load flat_dram(0::32)
      Foreach(0 until 10, 0 until 10) { (r,c) =>
        val window = Reduce(Reg[T](0.to[T]))(0 until 5 par 5, 0 until 5 par 5){ (i,j) => 
          dim2_sram(r+i,c+j) * flat_sram(i*5+j)
        }{_+_}
        tmp(r, c) = window.value
      }
      result_dram store tmp
    }
    
    val gold = Array[T](13400,13700,14000,14300,14600,14900,15200,15500,15800,16100,
17900,18200,18500,18800,19100,19400,19700,20000,20300,20600,
22400,22700,23000,23300,23600,23900,24200,24500,24800,25100,
26900,27200,27500,27800,28100,28400,28700,29000,29300,29600,
31400,31700,32000,32300,32600,-32636,-32336,-32036,-31736,-31436,
-29636,-29336,-29036,-28736,-28436,-28136,-27836,-27536,-27236,-26936,
-25136,-24836,-24536,-24236,-23936,-23636,-23336,-23036,-22736,-22436,
-20636,-20336,-20036,-19736,-19436,-19136,-18836,-18536,-18236,-17936,
-16136,-15836,-15536,-15236,-14936,-14636,-14336,-14036,-13736,-13436,
-11636,-11336,-11036,-10736,-10436,-10136,-9836,-9536,-9236,-8936 )
    val output = getMatrix(result_dram)
    printMatrix(output, "output")
    assert(gold == output.flatten)
    println(r"PASS: ${gold == output.flatten}")
  }
}
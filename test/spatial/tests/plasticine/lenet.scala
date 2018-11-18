package spatial.tests.plasticine

import spatial.dsl._


class lenetNoPar extends lenet {
  val batch_par = 1
  val conv1_par = 1
  val conv2_par = 1
  val mat1_par = 1
  val mat2_par = 1
}

class lenet1 extends lenet {
  val batch_par = 1
  val conv1_par = 1
  val conv2_par = 1
  val mat1_par = 2
  val mat2_par = 2
}

class lenet2 extends lenet {
  val batch_par = 1
  val conv1_par = 2
  val conv2_par = 2
  val mat1_par = 1
  val mat2_par = 1
}

class lenet3 extends lenet {
  val batch_par = 2
  val conv1_par = 1
  val conv2_par = 1
  val mat1_par = 1
  val mat2_par = 1
}


@spatial abstract class lenet extends PlasticineTest {

  val batch_par:Int
  val conv1_par:Int
  val conv2_par:Int
  val mat1_par:Int
  val mat2_par:Int

  val ip = 16

  type T = FixPt[TRUE,_16,_16] // Use higher precision for more accuracy
  val BATCH_SIZE = 3         // TODO: Make this an argin instead of hard-coded
  
  def lenet_Dec6[T:Num](
    i0: Array[T],
    c0: Array[T],
    c1: Array[T],
    c2: Array[T],
    c3: Array[T],
    c4: Array[T],
    c5: Array[T],
    c6: Array[T],
    c7: Array[T]
  ) : Matrix[T] = {

    val c0_DRAM = DRAM[T](20,5,16)
    val i0_DRAM = DRAM[T](BATCH_SIZE,28,32)
    val c1_DRAM = DRAM[T](32)
    val c2_DRAM = DRAM[T](50,5,16)
    val c3_DRAM = DRAM[T](64)
    val c4_DRAM = DRAM[T](100,4,4,4,64)

    val c5_DRAM = DRAM[T](100,5)
    val c6_DRAM = DRAM[T](10,100,5)

    val c7_DRAM = DRAM[T](32)
    val tmp5_DRAM = DRAM[T](BATCH_SIZE,32)

    val c0_reshaped = c0.reshape(20,1,25)
    val c0_new = (0::20, 0::1, 0::32){(i,j,k) => if (k < 25) c0_reshaped(i,j,k) else 0 };
    setMem(c0_DRAM, c0_new)

    val i0_reshaped = i0.reshape(BATCH_SIZE,28,28)
    val i0_new = (0::BATCH_SIZE, 0::28, 0::32){(i,k,l) => if (l < 28) i0_reshaped(i,k,l) else 0 };
    setMem(i0_DRAM, i0_new)
    
    setMem(c1_DRAM, c1)

    val c2_reshaped = c2.reshape(50,500)
    val c2_new = (0::50, 0::512){(i,j) => if (j < 500) c2_reshaped(i,j) else 0 };
    setMem(c2_DRAM, c2_new)
    
    setMem(c3_DRAM, c3)
    
    setMem(c4_DRAM, c4)
    
    setMem(c5_DRAM, c5)

    val c6_reshaped = c6.reshape(10,500)
    val c6_new = (0::10, 0::512){(i,j) => if (j < 500) c6_reshaped(i,j) else 0 };
    setMem(c6_DRAM, c6_new)
    
    setMem(c7_DRAM, c7)

    Accel {
    
      val c1_SRAM = SRAM[T](32)
      c1_SRAM load c1_DRAM(0::32 par ip)

      val c3_SRAM = SRAM[T](64)
      c3_SRAM load c3_DRAM(0::64 par ip)

      val c5_SRAM = SRAM[T](5,128)
      c5_SRAM load c5_DRAM(0::5,0::128 par ip)
      
      val c7_SRAM = SRAM[T](32)
      c7_SRAM load c7_DRAM(0::32 par ip)
      
      Foreach(BATCH_SIZE by 1 par batch_par) { batch_img =>

        // Move data on-chip
        val i0_SRAM = SRAM[T](28,32)
        i0_SRAM load i0_DRAM(batch_img, 0::28, 0::32 par ip)
        
        // Conv2D 1st layer
        val tmp1_SRAM = SRAM[T](20,12,12)
        val c0_RF = SRAM[T](20,5,16)
        c0_RF load c0_DRAM(0::20, 0::5, 0::16 par ip) // Loading kernel
        Foreach(20 by 1 par conv1_par) { outD_i => // out channels
          val nr = 28
          val nc = 28
          val kr = 5
          val kc = 5
          val or = 24
          val oc = 24
          val tmp1_SRAM_conv = SRAM[T](or/2,2,oc/2,2) // 24 x 24
          Foreach(0 until or by 2, 2 by 1) { (ro, ri) => // Convolution with 5 x 5 kernel
            Foreach(0 until oc by 2, 2 by 1) { (co,ci) => // Convolution with 5 x 5 kernel
              val window = Reduce(Reg[T](0.to[T]))(0 until kr, 0 until kc par 5){ (i,j) =>
                i0_SRAM(ro+ri+i,co+ci+j) * c0_RF(outD_i,i,j)
              }{_+_}
              tmp1_SRAM_conv(ro,ri,co,ci) = window.value
            }
          }
          Foreach(12 by 1, 12 by 1) { (i,j) => // Fused pulling + relu + biasAdd
            val out = Reduce(Reg[T](0.to[T]))(2 by 1, 2 by 1 par 2) { (ii, jj) =>
              max(0.to[T], tmp1_SRAM_conv(i,ii,j,jj) + c1_SRAM(outD_i))
            } { (x,y) => max(x,y) }
            tmp1_SRAM(outD_i, i, j) = out.value
          }
        }
        // Optimization: BiasAdd was merged into Conv2D above
        // Optimization: ReLU was merged into Conv2D above
        // Optimization: MaxPool was merged into Conv2D above

        // Conv2D 2nd layer
        val tmp2_SRAM = SRAM[T](50,4,4)
        val c2_RF = SRAM[T](50,5,16)
        c2_RF load c2_DRAM(0::50,0::5,0::16 par ip)                            // Banking error if parallelized
        //Foreach(50 by 1 par conv2_par) { outD_i => // out channels
        Foreach(64 by 1 par conv2_par) { outD_i => // out channels
          val nr = 12
          val nc = 12
          val kr = 5
          val kc = 5
          val or = 8
          val oc = 8
          val d = 20
          val tmp2_SRAM_conv = SRAM[T](or, oc)
          MemReduce(tmp2_SRAM_conv)(d by 1 par 1) { inD_i => // in channels   // Banking error if parallelized
            val result = SRAM[T](or, oc)
            Foreach(0 until or, 0 until oc par 1) { (r,c) =>
              val window = Reduce(Reg[T](0.to[T]))(0 until kr par 1, 0 until kc par 5){ (i,j) =>
                tmp1_SRAM(inD_i, r+i,c+j) * c2_RF(inD_i,i,j)
              }{_+_}
              result(r, c) = window.value
            }
            result
          }{_+_} // Reduce across in channels

          Foreach(4 by 1, 4 by 1) { (i,j) =>
            val out = Reduce(Reg[T](0.to[T]))(2 by 1 by 2, 2 by 1 by 2) { (ii, jj) =>
              max(0.to[T], tmp2_SRAM_conv(i*2 + ii, j*2 + jj) + c3_SRAM(outD_i))
            } { (x,y) => max(x,y) }
            tmp2_SRAM(outD_i, i, j) = out.value
          }
        }
        // Optimization: BiasAdd was merged into Conv2D above
        // Optimization: ReLU was merged into Conv2D above
        // Optimization: MaxPool was merged into Conv2D above

        // Reshape
        //val tmp3_SRAM = SRAM[T](4,4,50)
        //Foreach(50 by 1 par 10, 4 by 1, 4 by 1) { (j,i,k) =>
          //tmp3_SRAM(k,i,j) = tmp2_SRAM(j, i, k)
        //}

        // MatMul
        val tmp4_SRAM = SRAM[T](5,128)
        Foreach(128 by 1 par mat1_par){ out_i =>
          val c4_row_SRAM = SRAM[T](5,4,4,64)
          c4_row_SRAM load c4_DRAM(out_i, 0::5, 0::4, 0::4, 0::64 par ip)
          Foreach(5 by 1){block_i =>
            val prod = Reduce(Reg[T](0.to[T]))(50 by 1, 4 by 1, 4 by 1 par 4){ (j,i,k) =>
              //tmp3_SRAM(j,i,k) * c4_row_SRAM(block_i,j,i,k)
              tmp2_SRAM(i,i,j) * c4_row_SRAM(block_i,j,i,k)
            }{_+_}
            tmp4_SRAM(block_i,out_i) = max(0.to[T], prod.value + c5_SRAM(block_i,out_i))
          }
        }
        // Optimization: BiasAdd was merged into MatMul above
        // Optimization: ReLU was merged into MatMul above

        // MatMul
        val tmp5_SRAM = SRAM[T](32)
        val c6_row_SRAM = SRAM[T](10,5,128)
        c6_row_SRAM load c6_DRAM(0::10, 0::5, 0::128 par ip)
        Foreach(10 by 1 par mat2_par){out_i =>
          val prod = Reduce(Reg[T](0.to[T]))(5 by 1, 128 by ip){ (in_i,in_j) => 
            tmp4_SRAM(in_i,in_j) * c6_row_SRAM(out_i,in_i, in_j) 
          }{_+_}
          tmp5_SRAM(out_i) = prod.value + c7_SRAM(out_i)
        }
        // Optimization: BiasAdd was merged into MatMul above
        // Optimization: ReLU was merged into MatMul above

        tmp5_DRAM(batch_img, 0::32 par ip) store tmp5_SRAM
      } // Metapipeline over all images
    }
    
    getMatrix(tmp5_DRAM)
  }

  def main(args: Array[String]): Unit = {
    val i0 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/data_in2.csv", "\n")
    val c0 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c0.csv", "\n") // conv1/Variable
    val c1 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c1.csv", "\n") // conv1/Variable_1
    val c2 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c2.csv", "\n") // conv2/Variable
    val c3 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c3.csv", "\n") // conv2/Variable_1
    val c4 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c4.csv", "\n") // fc1/Variable
    val c5 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c5.csv", "\n") // fc1/Variable_1
    val c6 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c6.csv", "\n") // fc2/Variable
    val c7 = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/c7.csv", "\n") // fc2/Variable_1
    val output = lenet_Dec6(i0, c0, c1, c2, c3, c4, c5, c6, c7)
    val output_no_extra = Array.tabulate(170){i => output(i/10, i%10)}
    printArray(output_no_extra, "output")
    val gold = loadCSV1D[T]("/home/shadjis/spatial/DEVELOP_spatial-lang/csv_lenetDNNW/data_out.csv", "\n")
    printArray(gold, "gold")
    val margin = 1.882.to[T]
  	val cksum = gold.zip(output_no_extra){(a,b) => abs(a-b) < margin}.reduce{_&&_}
  	println("PASS: " + cksum)
    assert(cksum)
  }
}

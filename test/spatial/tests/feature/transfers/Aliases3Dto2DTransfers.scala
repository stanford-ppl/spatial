package spatial.tests.feature.transfers


import spatial.dsl._


@spatial class Aliases3Dto2DTransfers extends SpatialTest {
  override def runtimeArgs: Args = "400"

  /* 
   
   KEY: X = face with same values

        dim0
       /
       --> dim2
       |
      dim1
                        dram1
                     __________                                                
                    /         /|                                                 
                   /         / |                                        
                  /_________/  |                                                 
                 |          |  |                                        
                 |          |  |                                        
                 |    X     |  |                                        
                 |          |  |                                        
                 |          | /                                         
                 |__________|/         


      first rotation      second rotation                                                 
                                                                                    
           dram2                dram3                     
        __________           __________                          
       /         /|         /         /|                         
      /    X    / |        /         / |                         
     /_________/  |       /_________/  |                         
    |          |  |      |          |  |                         
    |          |  |      |          | X|                         
    |          |  |      |          |  |                         
    |          |  |      |          |  |                         
    |          | /       |          | /                          
    |__________|/        |__________|/                           
                                                                 

  */

  def main(args: Array[String]): Unit = {
    type T = Int64

    val src1 = (0::8,0::8,0::8){(i,j,k) => i.to[T]}
    val dram1 = DRAM[T](8,8,8)
    val dram2 = DRAM[T](8,8,8)
    val dram3 = DRAM[T](8,8,8)

    setMem(dram1, src1)

    Accel {
      val x1 = SRAM[T](8,8)
      val x2 = SRAM[T](8,8)
      Foreach(8 by 1){i => 
        x1 load dram1(i,0::8,0::8)
        dram2(0::8,i,0::8) store x1
      }
      Foreach(8 by 1){j => 
        x2 load dram1(j,0::8,0::8)
        dram3(0::8,0::8,j) store x2
      }
    }

    val rotate2 = (0::8,0::8,0::8){(i,j,k) => j.to[T]}
    val rotate3 = (0::8,0::8,0::8){(i,j,k) => k.to[T]}
    printTensor3(getTensor3(dram1), "Original")
    printTensor3(getTensor3(dram2), "Rotation 2")
    printTensor3(rotate2, "Gold Rotation 2")
    printTensor3(getTensor3(dram3), "Rotation 3")
    printTensor3(rotate3, "Gold Rotation 3")
    assert(getTensor3(dram2) === rotate2)
    assert(getTensor3(dram3) === rotate3)
  }
}

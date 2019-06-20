package spatial.tests.apps

import spatial.dsl._
@spatial class DRAMBits extends SpatialTest { // Test Args: 

  val size_b = 512
  		    
  def main(args: Array[String]): Void = {

    val bits_DRAM = DRAM[Bit](size_b)

    val stored_bits_DRAM = DRAM[Bit](size_b)
    setMem(bits_DRAM, Array.tabulate(size_b) { i => i.to[Int].bit(0).to[Bit] })
 
    Accel {

       val bits_SRAM = SRAM[Bit](size_b)
       bits_SRAM load bits_DRAM(0::size_b)
       stored_bits_DRAM(0::size_b) store bits_SRAM
    }

    val all_bits = getMem(stored_bits_DRAM)
    printArray(all_bits)  
  }
}  

package spatial.lang

trait DRAMDenseTile[A,Dram[T]<:DRAM[T,Dram]] {
  def dram: Dram[A]
  def ranges: Seq[Series[Idx]]
}

case class DRAMDenseTile1[A:Bits,Dram[T]<:DRAM[T,Dram]](dram: Dram[A], ranges: Seq[Series[Idx]]) extends DRAMDenseTile[A,Dram] {

}

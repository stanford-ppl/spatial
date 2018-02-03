package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._
import pcc.util.multiLoop

import scala.collection.mutable

case class MemoryConfigurer[C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State)
  extends AccessExpansion(mem)
{
  private val rank: Int = rankOf(mem)




}

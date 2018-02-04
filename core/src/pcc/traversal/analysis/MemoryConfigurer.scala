package pcc.traversal
package analysis

import pcc.core._
import pcc.data._
import pcc.lang._

class MemoryConfigurer[C[_]](mem: Mem[_,C], strategy: BankingStrategy)(implicit state: State) extends AccessExpansion(mem) {
  protected val rank: Int = rankOf(mem)

  


}

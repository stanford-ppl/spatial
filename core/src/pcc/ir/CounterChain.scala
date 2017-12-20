package pcc
package ir

import forge._

/** Types **/
case class CounterChain(eid: Int) extends Box[CounterChain](eid) {
  override type I = Array[Range]

  override def fresh(id: Int): CounterChain = CounterChain(id)
  override def stagedClass: Class[CounterChain] = classOf[CounterChain]
}
object CounterChain {
  implicit val cchain: CounterChain = CounterChain(-1)
}

/** Nodes **/
case class CounterChainAlloc(counters: Seq[Counter]) extends BoxAlloc[CounterChain]


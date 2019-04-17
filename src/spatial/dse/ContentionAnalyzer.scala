package spatial.dse

import argon._
import argon.node._
import spatial.metadata.params._
import spatial.lang._
import spatial.node._
import spatial.lang.I32
import spatial.metadata.bounds._
import spatial.metadata.memory._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.types._
import spatial.util.spatialConfig

import scala.collection._

case class ContentionAnalyzer(IR: State) extends argon.passes.Traversal  {

  val isolatedContention = mutable.HashMap[Sym[_],Seq[Int]]()

  def childrenAndTransfers(x: Sym[_]): Seq[Sym[_]] = {
    val children = x.children.collect{case c if c.s.get != x => c.s.get}
    val transfers = x.blocks.flatMap(_.stms).collect{case x if x.isTileTransfer => x}
    children ++ transfers
  }
  def outerContention(x: Sym[_], P: => Int): Int = {
    if (childrenAndTransfers(x).nonEmpty) {
      val ics = childrenAndTransfers(x).map{c => calcContention(c) * P}
      isolatedContention(x) = ics
      if (x.isPipeControl || x.isStreamControl) ics.sum else ics.max
    }
    else 0
  }

  def calcContention(x: Sym[_]): Int = x match {
    case Def(_:AccelScope)            => outerContention(x, 1)
    case Def(_:ParallelPipe)       => childrenAndTransfers(x).map{c => calcContention(c)}.sum
    case Def(_:UnitPipe)           => outerContention(x, 1)
    case Def(e:OpForeach)          => outerContention(x, e.cchain.constPars.product)
    case Def(e:OpReduce[_])        => outerContention(x, e.cchain.constPars.product)
    case Def(e:OpMemReduce[_,_])   => outerContention(x, e.cchainMap.constPars.product)
    case Def(_:DenseTransfer[_,_,_]) => 1
    case Def(_:SparseTransfer[_,_])  => 1
    case _ => 0
  }

  def markPipe(x: Sym[_], parent: Int): Unit = {
    if (x.isPipeControl || x.isStreamControl) {
      childrenAndTransfers(x).map{c => markContention(c,parent) }
    }
    else if (x.isSeqControl && childrenAndTransfers(x).nonEmpty) {
      val ics = isolatedContention(x)
      val mx = ics.max
      // Can just skip case where mx = 0 - no offchip memory accesses in this sequential anyway
      if (mx > 0) childrenAndTransfers(x).zip(ics).collect{case (child,c) => markContention(child, (parent/mx)*c) }
    }
  }

  def markContention(x: Sym[_], parent: Int): Unit = x match {
    case Def(_:AccelScope)            => markPipe(x, parent)
    case Def(_:ParallelPipe)       => childrenAndTransfers(x).map{c => markContention(c,parent)}
    case Def(_:UnitPipe)           => markPipe(x, parent)
    case Def(_:OpForeach)          => markPipe(x, parent)
    case Def(_:OpReduce[_])        => markPipe(x, parent)
    case Def(_:OpMemReduce[_,_])   => markPipe(x, parent)
    case Def(_:DenseTransfer[_,_,_]) => x.contention = parent
    case Def(_:SparseTransfer[_,_])  => x.contention = parent
    case _ => // do nothing
  }

  def run(): Unit = {
    val c = calcContention(TopCtrl.get)
    markContention(TopCtrl.get, c)
  }

  protected def process[S:Type](block: Block[S]): Block[S] = {
    run()
    block
  }

}

package spatial.traversal

import argon._
import spatial.lang._
import spatial.node._
import spatial.util.modeling._
import spatial.metadata.control._
import spatial.metadata.access._
import spatial.metadata.memory._

case class BroadcastCleanupAnalyzer(IR: State) extends AccelTraversal {

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _:AccelScope => inAccel{ super.visit(lhs, rhs) }

    case _:Control[_] if lhs.isInnerControl => 
      lhs.op.get.blocks.foreach{block =>
        block.stms.reverse.foreach{sym => 
          sym match {
            case Op(_: BankedAccessor[_,_]) if sym.getPorts(0).isDefined => 
              val broadcast = sym.port.broadcast.exists(_>0)
              dbgs(s"Reader $sym is broadcast: $broadcast")
              sym.nestedInputs.foreach{in => 
                dbgs(s"  - Propogating info to $in")
                in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr & broadcast) else broadcast
              }
            case Op(_: Writer[_]) if sym.getPorts(0).isDefined => 
              val broadcast = sym.port.broadcast.exists(_>0)
              dbgs(s"Reader $sym is broadcast: $broadcast")
              sym.nestedInputs.foreach{in => 
                dbgs(s"  - Propogating info to $in")
                in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr & broadcast) else broadcast
              }              
            case _ if (sym.getBroadcastAddr.isDefined) => 
              dbgs(s"Node $sym has BroadcastAddress metadata defined as ${sym.isBroadcastAddr} ")
              sym.nestedInputs.foreach{in => 
                dbgs(s"  - Propogating info to $in")
                in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr & sym.isBroadcastAddr) else sym.isBroadcastAddr
              }
            case _ =>
          }
        }
      }

    case _ => super.visit(lhs,rhs)
  }

}

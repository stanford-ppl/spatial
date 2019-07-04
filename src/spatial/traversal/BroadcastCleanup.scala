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

    case _: MemAlloc[_,_] => lhs.isBroadcastAddr = false

    case _:Control[_] if lhs.isInnerControl => 
      lhs.op.get.blocks.foreach{block =>
        block.stms.reverse.foreach{sym => 
          sym match {
            case Op(_: StatusReader[_]) => sym.isBroadcastAddr = false
            case Op(_: BankedEnqueue[_]) => 
              sym.isBroadcastAddr = false
              sym.nestedInputs.foreach{in => in.isBroadcastAddr = false}
            case Op(_: BankedDequeue[_]) => 
              sym.isBroadcastAddr = false
              sym.nestedInputs.foreach{in => in.isBroadcastAddr = false}

            case Op(x: BankedReader[_]) if sym.getPorts(0).isDefined => 
              sym.port.broadcast.zipWithIndex.foreach{
                case (b, i) if (b > 0) => 
                  dbgs(s"  - Reader $sym, lane $i is broadcast")
                  if (x.bank.size > i) {
                    x.bank(i).map{y => (y.nestedInputs ++ x.bank(i)).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }}
                    (x.ofs(i).nestedInputs ++ Seq(x.ofs(i))).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }
                    x.enss(i).map{y => (y.nestedInputs ++ x.enss(i).toSeq).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }}
                  }
                case (b, i) if (b == 0) => 
                  dbgs(s"  - Reader $sym, lane $i is NOT broadcast")
                  if (x.bank.size > i) {
                    x.bank(i).map{y => (y.nestedInputs ++ x.bank(i)).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }}
                    (x.ofs(i).nestedInputs ++ Seq(x.ofs(i))).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }
                    x.enss(i).map{y => (y.nestedInputs ++ x.enss(i).toSeq).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }}
                  }
              }
            case Op(x: Writer[_]) if sym.getPorts(0).isDefined => 
              val broadcast = sym.port.broadcast.exists(_>0)
              dbgs(s"Reader $sym is broadcast: $broadcast")
              sym.nestedInputs.foreach{in => 
                dbgs(s"  - Propogating info to $in")
                in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr & broadcast) else broadcast
              }              
            case Op(x: BankedWriter[_]) if sym.getPorts(0).isDefined => 
              sym.port.broadcast.zipWithIndex.foreach{
                case (b, i) if (b > 0) => 
                  dbgs(s"  - Reader $sym, lane $i is broadcast")
                  if (x.bank.size > i) {
                    x.bank(i).map{y => (y.nestedInputs ++ x.bank(i)).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }}
                    (x.ofs(i).nestedInputs ++ Seq(x.ofs(i))).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }
                    x.enss(i).map{y => (y.nestedInputs ++ x.enss(i).toSeq).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = if (in.getBroadcastAddr.isDefined) (in.isBroadcastAddr) else true
                    }}
                  }
                case (b, i) if (b == 0) => 
                  dbgs(s"  - Reader $sym, lane $i is NOT broadcast")
                  if (x.bank.size > i) {
                    x.bank(i).map{y => (y.nestedInputs ++ x.bank(i)).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }}
                    (x.ofs(i).nestedInputs ++ Seq(x.ofs(i))).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }
                    x.enss(i).map{y => (y.nestedInputs ++ x.enss(i).toSeq).foreach{in => 
                      dbgs(s"  - Propogating info to $in")
                      in.isBroadcastAddr = false
                    }}
                  }
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

package spatial.traversal

import argon._
import spatial.metadata.access._
import spatial.metadata.control._
import spatial.metadata.memory._
import spatial.lang._
import spatial.node._
import utils.implicits.collections._



case class BufferRecompute(IR: State) extends BlkTraversal {
  override protected def preprocess[R](block: Block[R]): Block[R] = {
    super.preprocess(block)
  }

  override protected def postprocess[R](block: Block[R]): Block[R] = {
    super.postprocess(block)
  }

  private def hasPortConflicts(mem: Sym[_]): Seq[Sym[_]] = {
    val ports = mem.readers.toSeq.map(_.port)
    if (!(ports.distinct.size == ports.size)) {
      mem.readers.collect{case x if (ports.count(_ == x.port) > 1) => x}.toSeq
    } else Seq()
  }

  override protected def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = rhs match {
    case _: MemAlloc[_,_] if (!lhs.isFIFO & !lhs.isNonBuffer & !lhs.isStreamIn & !lhs.isStreamOut & !lhs.isArgIn & !lhs.isArgOut) => 
      // Recompute buffer depth
      if (lhs.getDuplicates.isDefined) {
        dbgs(s"Recomputing depth of $lhs")
        val (_, bufPorts, _) = computeMemoryBufferPorts(lhs, lhs.readers, lhs.writers)
        val depth = bufPorts.values.collect{case Some(p) => p}.maxOrElse(0) + 1
        if (depth != lhs.instance.depth) {
          dbgs(s"Memory $lhs had depth of ${lhs.instance.depth}, now has depth of $depth")
          lhs.duplicates = lhs.duplicates.map(_.updateDepth(depth)).toSeq
        }
      }
      // Resolve port conflicts, specifically for MemReduce after unrolling.. Delete once #98 is fixed
      while (hasPortConflicts(lhs).size > 0) {
        val moveMe = hasPortConflicts(lhs).head
        val maxMuxPort = lhs.readers.map(_.port.muxPort).max
        dbgs(s"Memory $lhs has conflicting read ports: Bumping $moveMe (${moveMe.port}) to muxPort ${maxMuxPort} + 1")
        moveMe.addPort(0, Seq(), Port(moveMe.port.bufferPort, maxMuxPort+1, moveMe.port.muxOfs, moveMe.port.castgroup, moveMe.port.broadcast))
      }
      super.visit(lhs,rhs)

    case _ => super.visit(lhs,rhs)

  }

}

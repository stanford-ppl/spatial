package spatial.traversal

import argon._
import spatial.metadata.control._
import spatial.metadata.blackbox._

trait AccelTraversal extends argon.passes.Traversal {
  protected var inAccel: Boolean = false
  protected var inBBox: Boolean = false
  protected var inReduce: Boolean = false
  protected def inHw: Boolean = inAccel || inBBox

  protected def inAccel[A](blk: => A): A = {
    val saveAccel = inAccel
    val saveBBox = inBBox
    val saveEnGen = config.enGen
    inAccel = true
    inBBox = false
    if (backend == "accel" || backend == "tree") config.enGen = true
    else config.enGen = false
    val result = blk
    inAccel = saveAccel
    inBBox = saveBBox
    config.enGen = saveEnGen
    result
  }

  protected def inBox[A](blk: => A): A = {
    val saveAccel = inAccel
    val saveBBox = inBBox
    val saveEnGen = config.enGen
    inAccel = false
    inBBox = true
    if (backend == "accel" || backend == "tree") config.enGen = true
    else config.enGen = false
    val result = blk
    inAccel = saveAccel
    inBBox = saveBBox
    config.enGen = saveEnGen
    result
  }

  protected def outsideAccel[A](blk: => A): A = {
    val saveAccel = inAccel
    val saveBBox = inBBox
    val saveEnGen = config.enGen
    inAccel = false
    inBBox = false
    if (backend == "accel") config.enGen = false
    else config.enGen = true
    val result = blk
    inAccel = saveAccel
    inBBox = saveBBox
    config.enGen = saveEnGen
    result
  }

  protected def getControlNodes(blks: Block[_]*): Seq[Sym[_]] = blks.flatMap(_.stms.filter(_.isControl))
  protected def getBlackboxes(blks: Block[_]*): Seq[Sym[_]] = blks.flatMap(_.stms.filter(_.isBlackboxImpl))

}

trait BlkTraversal extends AccelTraversal {
  // The current block (raw index)
  protected var blk: Blk = Blk.Host
  protected def inCtrl[A](c: Sym[_])(func: => A): A = inBlk(Blk.Node(c,-1)){ func }
  protected def inBox[A](c: Sym[_])(func: => A): A = inBlk(Blk.SpatialBlackbox(c)){ func }
  protected def inBlk[A](b: Blk)(func: => A): A = {
    val prevBlk = blk
    val saveAccel  = inAccel
    val saveBBox = inBBox
    blk = b
    inAccel = inAccel || b.s.exists(_.isAccel)
    inBBox = inBBox || b.s.exists(_.isBlackboxImpl)
    val result = func
    blk = prevBlk
    inAccel = saveAccel
    inBBox = saveBBox
    result
  }

  def advanceBlk(): Unit = blk = blk match {
    case Blk.Host => Blk.Host
    case Blk.SpatialBlackbox(s) => Blk.SpatialBlackbox(s)
    case Blk.Node(s,id) => Blk.Node(s,id+1)
  }

}


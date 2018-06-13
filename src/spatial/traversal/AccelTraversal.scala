package spatial.traversal

import argon._
import spatial.data._
import spatial.util._

trait AccelTraversal extends argon.passes.Traversal {
  protected var inHw: Boolean = false

  protected def inAccel[A](blk: => A): A = {
    val saveHW = inHw
    val saveEnGen = config.enGen
    inHw = true
    if (backend == "accel" || backend == "tree") config.enGen = true
    else config.enGen = false
    val result = blk
    inHw = saveHW
    config.enGen = saveEnGen
    result
  }

  protected def outsideAccel[A](blk: => A): A = {
    val saveHW = inHw
    val saveEnGen = config.enGen
    inHw = false
    if (backend == "accel") config.enGen = false
    else config.enGen = true
    val result = blk
    inHw = saveHW
    config.enGen = saveEnGen
    result
  }

}

trait BlkTraversal extends AccelTraversal {
  // The current block (raw index)
  protected var blk: Blk = Blk.Host
  protected def inCtrl[A](c: Sym[_])(func: => A): A = inBlk(Blk.Node(c,-1)){ func }
  protected def inBlk[A](b: Blk)(func: => A): A = {
    val prevBlk = blk
    val saveHW  = inHw
    blk = b
    inHw = inHw || b.s.exists(_.isAccel)
    val result = func
    blk = prevBlk
    inHw = saveHW
    result
  }

  def advanceBlk(): Unit = blk = blk match {
    case Blk.Host => Blk.Host
    case Blk.Node(s,id) => Blk.Node(s,id+1)
  }

}


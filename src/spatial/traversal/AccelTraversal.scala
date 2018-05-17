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
    if (backend == "accel") config.enGen = true
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
  protected var blk: Ctrl = Host       // Used to track which duplicates should be made in which blocks
  protected def withCtrl[A](c: Sym[_])(func: => A): A = withCtrl(c.toCtrl)(func)
  protected def withCtrl[A](b: Ctrl)(func: => A): A = {
    val prevBlk = blk
    val saveHW  = inHw
    blk = b
    inHw = inHw || b.isAccel
    val result = func
    blk = prevBlk
    inHw = saveHW
    result
  }

  def advanceBlock(): Unit = blk = blk match {
    case Host => Host
    case Controller(s,id) => Controller(s,id+1)
  }

}


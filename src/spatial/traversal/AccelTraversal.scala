package spatial.traversal

import argon._
import spatial.data._
import spatial.util._

trait AccelTraversal extends argon.passes.Traversal {
  protected var inHw: Boolean = false

  protected def inAccel[A](blk: => A): A = {
    val saveHW = inHw
    inHw = true
    val result = blk
    inHw = saveHW
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
    inHw = isAccel(b)
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


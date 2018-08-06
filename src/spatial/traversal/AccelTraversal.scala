package spatial.traversal

import argon._
import spatial.metadata.control._

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


trait ScopeTraversal extends AccelTraversal {
  // The current block (raw index)
  protected var blk: Blk = Blk.Host
  protected var scp: Scope = Scope.Host
  protected def inCtrl[A](c: Sym[_])(func: => A): A = {blk = Blk.Node(c,-1); inScope(Scope.Node(c,-1,-1)){ func }}
  protected def inScope[A](b: Scope)(func: => A): A = {
    val prevScope = scp
    val saveHW  = inHw
    scp = b
    inHw = inHw || b.s.exists(_.isAccel)
    val result = func
    scp = prevScope
    inHw = saveHW
    result
  }

  def advanceScope(): Unit = {
    blk = blk match {
      case Blk.Host => Blk.Host
      case Blk.Node(s,blk) => Blk.Node(s,blk+1)
    }
    scp = blk.toScope
  }

}

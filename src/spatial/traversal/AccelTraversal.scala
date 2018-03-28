package spatial.traversal

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

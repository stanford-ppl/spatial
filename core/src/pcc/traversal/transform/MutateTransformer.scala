package pcc.traversal
package transform

import pcc.core._
import pcc.data._

abstract class MutateTransformer extends SubstTransformer with Traversal {

  def updateMetadata(lhs: Sym[_]): Unit = {
    val data = metadata.all(lhs).map{case (k,m) => k -> mirror(m) }
    metadata.addOrRemoveAll(lhs, data)
  }

  def update[A](lhs: Sym[A], rhs: Op[A]): Sym[A] = {
    logs(s"$lhs = $rhs [Update]")
    try {
      rhs.update(f)
      // TODO: Rewrite rules
      updateMetadata(lhs)
      lhs
    }
    catch {case t: Throwable =>
      bug("Encountered exception while updating node")
      bug(s"$lhs = $rhs")
      throw t
    }
  }

}

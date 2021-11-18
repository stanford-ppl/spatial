package spatial.transform

import argon._
import argon.passes.Traversal

case class MetadataStripper[M<:Data[M]:Manifest](IR: argon.State) extends Traversal {
  private val metadataName = implicitly[Manifest[M]].toString()

  /** Called to run the main part of this traversal. */
  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    metadata[M](lhs) match {
      case Some(meta) =>
        dbgs(s"Removing $lhs[$metadataName] = $meta")
        metadata.clear[M](lhs)
      case None =>
    }
    super.visit(lhs, rhs)
  }
}

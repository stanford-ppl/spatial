package spatial.transform

import argon._
import argon.passes.Traversal

case class Stripper[M<:Data[M]:Manifest]() {
  val metadataName: String = implicitly[Manifest[M]].toString()
  def strip(sym: Sym[_]): Unit = {
    metadata[M](sym) match {
      case Some(meta) =>
        metadata.clear[M](sym)
      case None =>
    }
  }
}

object Stripper {
  def S[M<:Data[M]:Manifest]: Stripper[M] = Stripper[M]()
}

case class MetadataStripper(IR: argon.State, strippers: Stripper[_]*) extends Traversal {
  /** Called to run the main part of this traversal. */
  override def visit[A](lhs: Sym[A], rhs: Op[A]): Unit = {
    strippers foreach {_.strip(lhs)}
    super.visit(lhs, rhs)
  }

  override def preprocess[R](block: Block[R]): Block[R] = {
    dbgs(s"Stripping: ${strippers.map(_.metadataName).mkString(", ")}")
    super.preprocess(block)
  }
}

package pcc.core

import forge._
import scala.collection.mutable.HashSet

/**
  * Static object for capturing rewrite rules
  */
object rewrites {
  private def keyOf[A<:Op[_]:Manifest] = manifest[A].runtimeClass.asInstanceOf[Class[A]]

  // List of applicable rewrite rules
  private var rules: Map[Class[_],PartialFunction[(Op[_],SrcCtx,State),Option[Any]]] = Map.empty
  private val names = HashSet[String]()

  def add[O<:Op[_]:Manifest](name: String, rule: PartialFunction[(Op[_],SrcCtx,State),Option[Any]]): Unit = if (!names.contains(name)) {
    val key = keyOf[O]
    rules.get(key) match {
      case Some(pf) => rules += (key -> pf.orElse(rule))
      case None     => rules += (key -> rule)
    }
    names += name
  }

  def apply[A](op: Op[A])(implicit ctx: SrcCtx, state: State): Option[A] = rules.get(op.getClass).flatMap{pf =>
    val tuple = (op,ctx,state)
    if (pf.isDefinedAt(tuple)) {
      pf.apply(tuple).map(_.asInstanceOf[A])
    }
    else {
      dbg(s"No rewrite rules found for op $op")
      None
    }
  }
}

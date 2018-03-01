package core

import forge.tags._
import scala.collection.mutable.HashSet

/**
  * Static object for capturing rewrite rules
  */
object rewrites {
  private def keyOf[A<:Op[_]:Manifest] = manifest[A].runtimeClass.asInstanceOf[Class[A]]

  // List of applicable rewrite rules
  private var rules: Map[Class[_],PartialFunction[(Op[_],SrcCtx,State),Option[Sym[_]]]] = Map.empty
  private val names = HashSet[String]()

  def add[O<:Op[_]:Manifest](name: String, rule: PartialFunction[(Op[_],SrcCtx,State),Option[Sym[_]]]): Unit = if (!names.contains(name)) {
    val key = keyOf[O]
    rules.get(key) match {
      case Some(pf) => rules += (key -> pf.orElse(rule))
      case None     => rules += (key -> rule)
    }
    names += name
  }

  def apply[A:Type](op: Op[A])(implicit ctx: SrcCtx, state: State): Option[A] = rules.get(op.getClass).flatMap{pf =>
    val tuple = (op,ctx,state)
    if (pf.isDefinedAt(tuple)) {
      pf.apply(tuple) match {
        case Some(s) if s.tp <:< Type[A] => Some(s.asInstanceOf[A])
        case _ => None
      }
    }
    else {
      dbg(s"No rewrite rules found for op $op")
      None
    }
  }
}

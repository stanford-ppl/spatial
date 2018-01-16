package pcc.core

import forge._

object rewrite {
  private def keyOf[A<:Op[_]:Manifest] = manifest[A].runtimeClass.asInstanceOf[Class[A]]

  var rules: Map[Class[_],PartialFunction[Op[_],Any]] = Map.empty

  def add[O<:Op[_]:Manifest](rule: PartialFunction[O,Any]): Unit = {
    val key = keyOf[O]
    val rul = rule.asInstanceOf[PartialFunction[Op[_],Any]]
    rules.get(key) match {
      case Some(pf) => rules += (key -> pf.orElse(rul))
      case None     => rules += (key -> rul)
    }
  }

  @internal def apply[A](op: Op[A]): Option[A] = rules.get(op.getClass).flatMap{pf =>
    if (pf.isDefinedAt(op)) {
      Some(pf.apply(op).asInstanceOf[A])
    }
    else {
      dbg(s"No rewrite rules found for op $op")
      None
    }
  }

}

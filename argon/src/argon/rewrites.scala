package argon

import utils.implicits.collections._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Static object for capturing rewrite rules
  */
object rewrites {
  type RewriteRule = PartialFunction[(Op[_],SrcCtx,State),Option[Sym[_]]]

  private def keyOf[A<:Op[_]:Manifest] = manifest[A].runtimeClass.asInstanceOf[Class[A]]

  // Roughly O(G), where G is the total number of global rewrite rules
  // When possible, use rules instead of globals
  private var globals: ArrayBuffer[RewriteRule] = ArrayBuffer.empty

  // Roughly O(R), where R is the number of rules for a specific node class
  private val rules: mutable.HashMap[Class[_], ArrayBuffer[RewriteRule]] = mutable.HashMap.empty
  private val names: mutable.HashSet[String] = mutable.HashSet.empty

  def rule(op: Op[_]): Seq[RewriteRule] = rules.getOrElse(op.getClass, Nil)

  def addGlobal(name: String, rule: RewriteRule): Unit = if (!names.contains(name)) {
    names += name
    globals += rule
  }

  def add[O<:Op[_]:Manifest](name: String, rule: RewriteRule): Unit = if (!names.contains(name)) {
    names += name
    val key = keyOf[O]
    val pfs = rules.getOrElseAdd(key, () => ArrayBuffer.empty[RewriteRule])
    pfs += rule
  }

  private def applyRule[A:Type](op: Op[A], ctx: SrcCtx, state: State, rule: RewriteRule): Option[A] = {
    rule.apply((op,ctx,state)) match {
      case Some(s) if s.tp <:< Type[A] => Some(s.asInstanceOf[A])
      case _ => None
    }
  }

  def apply[A:Type](op: Op[A])(implicit ctx: SrcCtx, state: State): Option[A] = {
    Option(op.rewrite)
          .orElse{ rule(op).mapFind{rule => applyRule[A](op,ctx,state, rule) } }
          .orElse{ globals.mapFind{rule => applyRule[A](op,ctx,state, rule) } }
  }
}

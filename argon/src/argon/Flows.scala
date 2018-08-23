package argon

import scala.collection.mutable.{ArrayBuffer,HashSet}

import utils.Instrument

trait FlowRules {
  val IR: State

}

/**
  * Static object for capturing simple forward dataflow analyses
  */
class Flows {
  private var rules = ArrayBuffer[(String,PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit])]()
  private[argon] var names = HashSet[String]()

  lazy val instrument = new Instrument("flows")

  def prepend(name: String, func: PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit]): Unit = {
    rules.prepend((name,func))
    names += name
  }

  def add(name: String, func: PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit]): Unit = {
    rules += ((name,func))
    names += name
  }
  def remove(name: String): Unit = {
    val idx = rules.indexWhere(_._1 == name)
    rules.remove(idx)
    names.remove(name)
  }

  def apply[A](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx, state: State): Unit = {
    val tuple = (lhs,rhs,ctx,state)
    rules.foreach{case (name,rule) =>
      if (rule.isDefinedAt(tuple)) { instrument(name){ rule.apply(tuple) } }
    }
  }

  def save(): Flows = {
    val flows = new Flows
    flows.rules ++= rules
    flows.names ++= names
    flows
  }
  def restore(flow: Flows): Unit = {
    rules = flow.rules
    names = flow.names
  }
}

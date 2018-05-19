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
  private val rules = ArrayBuffer[(String,PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit])]()
  private[argon] val names = HashSet[String]()

  lazy val instrument = new Instrument("flows")

  def add(name: String, func: PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit]): Unit = if (!names.contains(name)) {
    rules += ((name,func))
    names += name
  }

  def apply[A](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx, state: State): Unit = {
    val tuple = (lhs,rhs,ctx,state)
    rules.foreach{case (name,rule) =>
      if (rule.isDefinedAt(tuple)) { instrument(name){ rule.apply(tuple) } }
    }
  }
}

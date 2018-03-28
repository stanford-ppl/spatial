package argon

import scala.collection.mutable.{ArrayBuffer,HashSet}

trait FlowRules {
  val IR: State
}

/**
  * Static object for capturing simple forward dataflow analyses
  */
class Flows {
  private val rules = ArrayBuffer[(String,PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit])]()
  private val ruleNames = HashSet[String]()

  def add(name: String, func: PartialFunction[(Sym[_],Op[_],SrcCtx,State),Unit]): Unit = if (!ruleNames.contains(name)) {
    //println(s"Added flow rule: $name")
    rules += ((name,func))
    ruleNames += name
  }

  def apply[A](lhs: Sym[A], rhs: Op[A])(implicit ctx: SrcCtx, state: State): Unit = {
    val tuple = (lhs,rhs,ctx,state)
    rules.foreach{case (name,rule) =>
      //dbgs(s"Applying rule $name")
      if (rule.isDefinedAt(tuple)) rule.apply(tuple)
    }
  }
}

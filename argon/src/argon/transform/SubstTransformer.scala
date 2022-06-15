package argon
package transform

import utils.tags.instrument

trait Substitution {
  def resolve: Sym[_]
}

case class FuncSubst(func: () => Sym[_]) extends Substitution {
  override def resolve: Sym[_] = func()
}

case class DirectSubst(v: Sym[_]) extends Substitution {
  override def resolve: Sym[_] = v
}

/** An IR transformation pass which tracks substitutions using a scoped hash map. */
abstract class SubstTransformer extends Transformer {
//  var subst: Map[Sym[_],Sym[_]] = Map.empty
  var blockSubst: Map[Block[_],Block[_]] = Map.empty
//  var delayedSubst: Map[Sym[_], () => Sym[_]] = Map.empty

  var subst: Map[Sym[_], Substitution] = Map.empty

  type TransformerStateBundle = Seq[TransformerState]

  trait TransformerState {
    def restore(): Unit
  }
  case class SubstTransformerState(s: Map[Sym[_], Substitution], b: Map[Block[_],Block[_]]) extends TransformerState {
    override def restore(): Unit = {
      subst = s
      blockSubst = b
    }
  }

  def saveSubsts(): TransformerStateBundle = Seq(SubstTransformerState(subst, blockSubst))
  def restoreSubsts(substData: TransformerStateBundle): Unit = substData.foreach(_.restore())

  /** Register a substitution rule.
    * Usage: register(a -> a').
    */
  def register[A, B](rule: (A,B)): Unit = register(rule._1,rule._2)
  def register[A, B](seq: Seq[(A, B)]): Unit = seq foreach { case (a, b) => register(a, b)}

  var printRegister: Boolean = false
  /** Register a substitution rule orig -> sub. */
  def register[A,B](orig: A, sub: B): Unit = {
    if (printRegister) { dbgs(s"Register: $orig -> $sub") }
    (orig, sub) match {
      case (s1: Sym[_], s2: Sym[_]) => subst += s1 -> DirectSubst(s2)
      case (s1: Sym[_], s2: (() => Sym[_])) => subst += s1 -> FuncSubst(s2)
      case (s1: Seq[_], s2: Seq[_]) =>
        if (s1.size != s2.size) {
          throw new Exception(s"Mismatched registration sizes: ${s1.size} != ${s2.size}")
        }
        (s1 zip s2) foreach { case (a, b) => register(a, b) }
      case (b1: Block[_], b2: Block[_]) => blockSubst += b1 -> b2
      case _ => throw new Exception(s"Cannot register ${orig.getClass} -> ${sub.getClass}")
    }
  }

  /** Defines the substitution rule for a symbol s, i.e. the result of f(s). */
  final override protected def substituteSym[T](s: Sym[T]): Sym[T] = subst.get(s) match {
    case Some(s2) =>
      val result = s2.resolve.asInstanceOf[Sym[T]]
      result
    case None     => s
  }

  /** Defines the substitution rule for a block b, i.e. the result of f(b). */
  final override protected def substituteBlock[T](b: Block[T]): Block[T] = blockSubst.get(b) match {
    case Some(b2) => b2.asInstanceOf[Block[T]]
    case None     =>
      isolateSubst(b.result) {
        stageScope(f(b.inputs),b.options){ inlineBlock(b) }
      }
  }

  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolateSubstWith[A](escape: Seq[Sym[_]], rules: (Sym[_],Sym[_])*)(scope: => A): A = {
    isolateSubst(escape:_*){
      rules.foreach{rule => register(rule) }
      scope
    }
  }

  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolateSubstWith[A](escape: Seq[Sym[_]], rules: Map[Sym[_],Sym[_]])(scope: => A): A = {
    isolateSubstWith(escape, rules.toSeq:_*){ scope }
  }

  /** Conditionally isolate all substitution rules created within the given scope.
    * If cond is true, substitution rules are reset at the end of this scope
    * except for any symbols explicitly listed in escape.
    * If false, all substitution rules persist.
    */
  def isolateSubstIf[A](cond: Boolean, escape: Seq[Sym[_]])(block: => A): A = {
    val save = subst
    //dbgs("[Enter] Escape: " + escape.mkString(","))
    //dbgs("[Enter] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    val result = block
    //dbgs("[Inside] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    if (cond) {
      subst = save ++ subst.filter{case (s1,_) => escape.contains(s1) }
    }
    //dbgs("[Exit] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    result
  }

  /** Isolate all substitution rules created within the given scope.
    * Substitution rules are reset at the end of this scope.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolateSubst[A](escape: Sym[_]*)(scope: => A): A = isolateSubstIf(cond=true, escape){ scope }

  /**
    * Preserve all substitution rules with a few exceptions
    * @param exclude: symbols to not export from the scope
    * @param block:
    * @tparam A
    * @return
    */
  def excludeSubst[A](exclude: Sym[_]*)(block: => A): A = {
    val save = subst
    val result = block
    subst = save ++ subst.filterNot { case (s, _) => exclude.contains(s) }
    result
  }


  override protected def blockToFunction0[R](block: Block[R]): () => R = {
    () => isolateSubst(){
      inlineBlock(block).unbox
    }
  }

  final override protected def lambda1ToFunction1[A,R](lambda1: Lambda1[A,R]): A => R = {
    {a: A => isolateSubst() {
      register(lambda1.input -> a)
      val block = blockToFunction0(lambda1)
      block()
    }}
  }
  final override protected def lambda2ToFunction2[A,B,R](lambda2: Lambda2[A,B,R]): (A,B) => R = {
    {(a: A, b: B) => isolateSubst() {
      register(lambda2.inputA -> a)
      register(lambda2.inputB -> b)
      dbgs(s"Creating Function0 with subst: {${lambda2.inputA} -> $a, ${lambda2.inputB} -> $b}")
      val block = blockToFunction0(lambda2)
      dbgs(s"Executing Function0")
      block()
    }}
  }
  final override protected def lambda3ToFunction3[A,B,C,R](lambda3: Lambda3[A,B,C,R]): (A,B,C) => R = {
    { (a: A, b: B, c: C) => isolateSubst() {
      register(lambda3.inputA -> a)
      register(lambda3.inputB -> b)
      register(lambda3.inputC -> c)
      val block = blockToFunction0(lambda3)
      block()
    }}
  }

  override protected def preprocess[S](block: Block[S]): Block[S] = {
    subst = Map.empty         // No substitutions should persist across runs
    super.preprocess(block)
  }
}

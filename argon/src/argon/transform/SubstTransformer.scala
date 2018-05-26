package argon
package transform

import utils.tags.instrument

/** An IR transformation pass which tracks substitutions using a scoped hash map. */
abstract class SubstTransformer extends Transformer {
  var subst: Map[Sym[_],Sym[_]] = Map.empty
  var blockSubst: Map[Block[_],Block[_]] = Map.empty

  /** Register a substitution rule.
    * Usage: register(a -> a').
    */
  def register[A](rule: (A,A)): Unit = register(rule._1,rule._2)

  /** Register a substitution rule orig -> sub. */
  def register[A,B](orig: A, sub: B): Unit = (orig, sub) match {
    case (s1: Sym[_], s2: Sym[_])       => subst += s1 -> s2
    case (b1: Block[_], b2: Block[_])   => blockSubst += b1 -> b2
    case _ => throw new Exception(s"Cannot register ${orig.getClass} -> ${sub.getClass}")
  }

  /** Defines the substitution rule for a symbol s, i.e. the result of f(s). */
  final override protected def substituteSym[T](s: Sym[T]): Sym[T] = subst.get(s) match {
    case Some(s2) => s2.asInstanceOf[Sym[T]]
    case None     => s
  }

  /** Defines the substitution rule for a block b, i.e. the result of f(b). */
  final override protected def substituteBlock[T](b: Block[T]): Block[T] = blockSubst.get(b) match {
    case Some(b2) => b2.asInstanceOf[Block[T]]
    case None     =>
      isolate(b.result) {
        stageScope(f(b.inputs),b.options){ inlineBlock(b) }
      }
  }

  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolateWith[A](escape: Seq[Sym[_]], rules: (Sym[_],Sym[_])*)(scope: => A): A = {
    isolate(escape:_*){
      rules.foreach{rule => register(rule) }
      scope
    }
  }

  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolateWith[A](escape: Seq[Sym[_]], rules: Map[Sym[_],Sym[_]])(scope: => A): A = {
    isolateWith(escape, rules.toSeq:_*){ scope }
  }

  /** Conditionally isolate all substitution rules created within the given scope.
    * If cond is true, substitution rules are reset at the end of this scope
    * except for any symbols explicitly listed in escape.
    * If false, all substitution rules persist.
    */
  def isolateIf[A](cond: Boolean, escape: Seq[Sym[_]])(block: => A): A = {
    val save = subst
    //dbgs("[Enter] Escape: " + escape.mkString(","))
    //dbgs("[Enter] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    val result = block
    //dbgs("[Inside] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    if (cond) subst = save ++ subst.filter{case (s1,_) => escape.contains(s1) }
    //dbgs("[Exit] Subst: " + subst.map{case (s1,s2) => s"$s1->$s2"}.mkString(","))
    result
  }

  /** Isolate all substitution rules created within the given scope.
    * Substitution rules are reset at the end of this scope.
    * Any symbols in escape have their substitution rules preserved from within this scope.
    */
  def isolate[A](escape: Sym[_]*)(scope: => A): A = isolateIf(cond=true, escape){ scope }


  override protected def blockToFunction0[R](block: Block[R]): () => R = {
    () => isolate(){
      inlineBlock(block).unbox
    }
  }

  final override protected def lambda1ToFunction1[A,R](lambda1: Lambda1[A,R]): A => R = {
    {a: A => isolate() {
      register(lambda1.input -> a)
      val block = blockToFunction0(lambda1)
      block()
    }}
  }
  final override protected def lambda2ToFunction2[A,B,R](lambda2: Lambda2[A,B,R]): (A,B) => R = {
    {(a: A, b: B) => isolate() {
      register(lambda2.inputA -> a)
      register(lambda2.inputB -> b)
      dbgs(s"Creating Function0 with subst: {${lambda2.inputA} -> $a, ${lambda2.inputB} -> $b}")
      val block = blockToFunction0(lambda2)
      dbgs(s"Executing Function0")
      block()
    }}
  }
  final override protected def lambda3ToFunction3[A,B,C,R](lambda3: Lambda3[A,B,C,R]): (A,B,C) => R = {
    { (a: A, b: B, c: C) => isolate() {
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

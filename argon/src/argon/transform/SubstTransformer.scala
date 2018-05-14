package argon
package transform

abstract class SubstTransformer extends Transformer {
  var subst: Map[Sym[_],Sym[_]] = Map.empty
  var substLazy: Map[Sym[_], () => Sym[_]] = Map.empty
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

  /** Register a lazy substitution rule orig -> sub, where sub is created on demand. */
  def registerLazy[A,B](orig: Sym[_], sub: () => Sym[_]): Unit = substLazy += orig -> sub

  override protected def transformBlock[T](block: Block[T]): Block[T] = blockSubst.get(block) match {
    case Some(block2) => block2.asInstanceOf[Block[T]]
    case None         => super.transformBlock(block)
  }

  override protected def transformSym[T](sym: Sym[T]): Sym[T] = subst.get(sym) match {
    case Some(y) => y.asInstanceOf[Sym[T]]
    case None    => substLazy.get(sym) match {
      case Some(s) => s().asInstanceOf[Sym[T]]
      case None    => sym
    }
  }


  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    */
  def isolateSubstWith[A](rules: (Sym[_],Sym[_])*)(scope: => A): A = {
    isolate {
      rules.foreach{rule => register(rule) }
      scope
    }
  }

  /** Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    */
  def isolateSubstWith[A](rules: Map[Sym[_],Sym[_]])(scope: => A): A = {
    isolateSubstWith(rules.toSeq:_*){ scope }
  }

  /** Conditionally isolate all substitution rules created within the given scope.
    * If cond is true, substitution rules are reset at the end of this scope.
    */
  def isolateIf[A](cond: Boolean)(block: => A): A = {
    val save = subst
    val result = block
    if (cond) subst = save
    result
  }

  /** Isolate all substitution rules created within the given scope.
    * Substitution rules are reset at the end of this scope.
    */
  def isolate[A](scope: => A): A = isolateIf(cond=true){ scope }


  override protected def blockToFunction0[R](b: Block[R], copy: Boolean): () => R = {
    () => isolateIf(copy){
      inlineBlock(b).unbox
    }
  }
  final override protected def lambda1ToFunction1[A,R](lambda1: Lambda1[A,R], copy: Boolean): A => R = {
    {a: A => isolateIf(copy) {
      register(lambda1.input -> a)
      val block = blockToFunction0(lambda1, copy)
      block()
    }}
  }
  final override protected def lambda2ToFunction2[A,B,R](lambda2: Lambda2[A,B,R], copy: Boolean): (A,B) => R = {
    {(a: A, b: B) => isolateIf(copy) {
      register(lambda2.inputA -> a)
      register(lambda2.inputB -> b)
      val block = blockToFunction0(lambda2, copy)
      block()
    }}
  }
  final override protected def lambda3ToFunction3[A,B,C,R](lambda3: Lambda3[A,B,C,R], copy: Boolean): (A,B,C) => R = {
    { (a: A, b: B, c: C) => isolateIf(copy) {
      register(lambda3.inputA -> a)
      register(lambda3.inputB -> b)
      register(lambda3.inputC -> c)
      val block = blockToFunction0(lambda3, copy)
      block()
    }}
  }

}

package core
package transform

abstract class SubstTransformer extends Transformer {
  val allowUnsafeSubst: Boolean = false
  val allowOldSymbols: Boolean = false

  var subst: Map[Sym[_],Sym[_]] = Map.empty
  var blockSubst: Map[Block[_],Block[_]] = Map.empty

  /**
    * Register a substitution rule.
    * Usage: register(a -> a').
    */
  def register[A](rule: (A,A)): Unit = register(rule._1,rule._2)

  /**
    * Register an unsafe substitution rule.
    * where a' replaces a but a' is not a subtype of a.
    */
  def registerUnsafe[A,B](rule: (A,B)): Unit = register(rule._1,rule._2,unsafe = true)

  /**
    * Register a substitution rule orig -> sub.
    * If unsafe is true, does not do type checking.
    */
  def register[A,B](orig: A, sub: B, unsafe: Boolean = allowUnsafeSubst): Unit = (orig,sub) match {
    case (s1: Sym[_], s2: Sym[_]) =>
      if (s2.tp <:< s1.tp || unsafe) subst += s1 -> s2
      else throw new Exception(s"Substitution $s1 -> $s2: ${s2.tp} is not a subtype of ${s1.tp}")

    case (b1: Block[_], b2: Block[_]) =>
      if (b2.result.tp <:< b1.result.tp || unsafe) blockSubst += b1 -> b2
      else throw new Exception(s"Substitution $b1 -> $b2: ${b2.result.tp} is not a subtype of ${b1.result.tp}")

    case _ => throw new Exception(s"Cannot register ${orig.getClass}, ${sub.getClass}")
  }

  override protected def transformBlock[T](block: Block[T]): Block[T] = blockSubst.get(block) match {
    case Some(block2) => block2.asInstanceOf[Block[T]]
    case None => stageScope(f(block.inputs),block.options){ inlineBlock(block) }
  }


  override protected def transformSym[T](sym: Sym[T]): Sym[T] = subst.get(sym) match {
    case Some(y) => y.asInstanceOf[Sym[T]]
    case None if sym.isSymbol && !allowOldSymbols =>
      throw new Exception(s"Used untransformed symbol $sym!")
    case None => sym
  }

  /**
    * Isolate all substitution rules created within the given scope.
    * Substitution rules are reset at the end of this scope.
    */
  def isolateSubst[A](scope: => A): A = {
    val save = subst
    val result = scope
    subst = save
    result
  }

  /**
    * Isolate the substitution rules created within the given scope,
    * with the given rule(s) added within the scope prior to evaluation.
    */
  def isolateSubstWith[A](rules: (Sym[_],Sym[_])*)(scope: => A): A = {
    isolateSubst{
      rules.foreach{rule => register(rule) }
      scope
    }
  }
  def isolateSubstWith[A](rules: Map[Sym[_],Sym[_]])(scope: => A): A = {
    isolateSubstWith(rules.toSeq:_*){ scope }
  }

  def isolateIf[A](cond: Boolean)(block: => A): A = {
    val save = subst
    val result = block
    if (cond) subst = save
    result
  }

  final override protected def blockToFunction0[R](b: Block[R], copy: Boolean): () => R = {
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

package argon

import Freq._

sealed class Block[R](
  val inputs:  Seq[Sym[_]], // External inputs to this block
  val stms:    Seq[Sym[_]], // All statements in this scope (linearized graph)
  val result:  Sym[R],      // The symbolic result of this block
  val effects: Effects,     // All external effects of this block
  val options: BlockOptions // Other settings for this block
) {
  def tp: Type[R] = result.tp
  def temp: Freq = options.temp

  def nestedStms: Seq[Sym[_]] = stms ++ stms.flatMap{s =>
    s.op.map{o => o.blocks.flatMap(_.nestedStms) }.getOrElse(Nil)
  }

  def nestedInputs: Set[Sym[_]] = nestedStmsAndInputs._2

  def nestedStmsAndInputs: (Set[Sym[_]], Set[Sym[_]]) = {
    val stms = this.nestedStms.toSet
    val used = stms.flatMap(_.inputs) ++ inputs
    val made = stms.flatMap{s => s +: s.op.map(_.binds).getOrElse(Nil) }
    val ins  = (used diff made).filterNot(_.isValue)
    (stms, ins)
  }

  override def toString: String = {
    if (inputs.isEmpty) s"Block($result)" else s"""Block(${inputs.mkString("(", ",", ")")} => $result)"""
  }
  override def hashCode() = (inputs, result, effects, options).hashCode()
  override def equals(x: Any): Boolean = x match {
    case that: Block[_] =>
        that.result == this.result && that.effects == this.effects &&
        that.inputs == this.inputs && that.options == this.options
    case _ => false
  }

  def asLambda1[A]: Lambda1[A,R] = Lambda1[A,R](
    inputs(0).asInstanceOf[Sym[A]],
    stms, result, effects, options
  )
  def asLambda2[A,B]: Lambda2[A,B,R] = Lambda2[A,B,R](
    inputs(0).asInstanceOf[Sym[A]],
    inputs(1).asInstanceOf[Sym[B]],
    stms, result, effects, options
  )
  def asLambda3[A,B,C]: Lambda3[A,B,C,R] = Lambda3[A,B,C,R](
    inputs(0).asInstanceOf[Sym[A]],
    inputs(1).asInstanceOf[Sym[B]],
    inputs(2).asInstanceOf[Sym[C]],
    stms, result, effects, options
  )
}

case class Lambda1[A,R](
    input: Sym[A],
    override val stms:    Seq[Sym[_]],
    override val result:  Sym[R],
    override val effects: Effects,
    override val options: BlockOptions)
  extends Block[R](Seq(input),stms,result,effects,options)


case class Lambda2[A,B,R](
    inputA: Sym[A],
    inputB: Sym[B],
    override val stms:    Seq[Sym[_]],
    override val result:  Sym[R],
    override val effects: Effects,
    override val options: BlockOptions)
  extends Block[R](Seq(inputA,inputB),stms,result,effects,options)


case class Lambda3[A,B,C,R](
    inputA: Sym[A],
    inputB: Sym[B],
    inputC: Sym[C],
    override val stms:    Seq[Sym[_]],
    override val result:  Sym[R],
    override val effects: Effects,
    override val options: BlockOptions)
  extends Block[R](Seq(inputA,inputB,inputC),stms,result,effects,options)


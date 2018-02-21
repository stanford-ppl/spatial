package core

package object static {
  type State = core.State
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
  type Config = core.Config
  type Issue  = core.Issue

  type Type[T] = core.Type[T]
  type Sym[T] = core.Sym[T]
  type Top[T] = core.Top[T]

  type Def[+A,+B] = core.Def[A,B]
  lazy val Def = core.Def

  type Op[T] = core.Op[T]
  lazy val Op = core.Op

  type Block[T] = core.Block[T]
  lazy val Block = core.Block

  type BlockOptions = core.BlockOptions
  lazy val BlockOptions = core.BlockOptions

  lazy val Stm = core.Stm

  type Impure = core.Impure
  lazy val Impure = core.Impure

  type Effects = core.Effects
  lazy val Effects = core.Effects
  lazy val effectsOf = core.effectsOf
  lazy val isMutable = core.isMutable

  lazy val metadata = core.metadata
  lazy val rewrites = core.rewrites
  lazy val globals = core.globals
  lazy val flows = core.flows
}

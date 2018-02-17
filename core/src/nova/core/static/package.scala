package nova.core

import java.io.PrintStream

package object static {
  type State = nova.core.State
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
  type Config = nova.core.Config
  type Issue  = nova.core.Issue

  type Type[T] = nova.core.Type[T]
  type Sym[T] = nova.core.Sym[T]
  type Top[T] = nova.core.Top[T]

  type Def[+A,+B] = nova.core.Def[A,B]
  lazy val Def = nova.core.Def

  type Op[T] = nova.core.Op[T]
  lazy val Op = nova.core.Op

  type Block[T] = nova.core.Block[T]
  lazy val Block = nova.core.Block

  type BlockOptions = nova.core.BlockOptions
  lazy val BlockOptions = nova.core.BlockOptions

  lazy val Stm = nova.core.Stm

  type Impure = nova.core.Impure
  lazy val Impure = nova.core.Impure

  type Effects = nova.core.Effects
  lazy val Effects = nova.core.Effects
  lazy val effectsOf = nova.core.effectsOf
  lazy val isMutable = nova.core.isMutable

  lazy val metadata = nova.core.metadata
  lazy val rewrites = nova.core.rewrites
  lazy val globals = nova.core.globals
  lazy val flows = nova.core.flows
}

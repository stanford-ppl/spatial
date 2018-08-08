package argon

/** Type and object aliases only for use within the argon.static package. */
package object static {
  type State = argon.State
  type Config = argon.Config
  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
  type Issue = argon.Issue

  type Sym[+T] = argon.Sym[T]
  type Exp[+C,+T] = argon.Exp[C,T]
  type Ref[+C,+T] = argon.Ref[C,T]

  type ExpType[+C,T] = argon.ExpType[C,T]
  type Type[T] = argon.Type[T]
  lazy val Type = argon.Type

  type Cast2Way[A,B] = argon.Cast2Way[A,B]
  type CastFunc[A,B] = argon.CastFunc[A,B]
  type Lifter[A,B] = argon.Lifter[A,B]

  type Def[+C,+T] = argon.Def[C,T]
  lazy val Def = argon.Def

  type Op[T] = argon.Op[T]
  lazy val Op = argon.Op
  type AtomicRead[T] = argon.AtomicRead[T]

  type Block[T] = argon.Block[T]
  type Lambda1[A,T] = argon.Lambda1[A,T]
  type Lambda2[A,B,T] = argon.Lambda2[A,B,T]
  type Lambda3[A,B,C,T] = argon.Lambda3[A,B,C,T]
  type BlockOptions = argon.BlockOptions
  lazy val BlockOptions = argon.BlockOptions

  lazy val metadata = argon.metadata
  lazy val globals = argon.globals

  type Consumers = argon.Consumers
  lazy val Consumers = argon.Consumers
  type Effects = argon.Effects
  lazy val Effects = argon.Effects
  type Impure = argon.Impure
  lazy val Impure = argon.Impure
  type NestedInputs = argon.NestedInputs
  lazy val NestedInputs = argon.NestedInputs

  type ShallowAliases = argon.ShallowAliases
  lazy val ShallowAliases = argon.ShallowAliases
  type DeepAliases = argon.DeepAliases
  lazy val DeepAliases = argon.DeepAliases
}

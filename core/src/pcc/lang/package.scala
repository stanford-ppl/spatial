package pcc

package object lang extends static.Statics with static.InternalAliases {
  type App = compiler.App
  type State = core.State

  type SrcCtx = forge.SrcCtx
  lazy val SrcCtx = forge.SrcCtx
}


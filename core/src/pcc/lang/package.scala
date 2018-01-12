package pcc

package object lang extends static.Statics with static.InternalAliases {
  type App = compiler.App
  type State = core.State

  type SrcCtx = core.SrcCtx
  def SrcCtx = core.SrcCtx

  implicit def ctx: SrcCtx = SrcCtx.empty
}
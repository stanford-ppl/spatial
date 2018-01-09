package pcc

object lang extends pcc.ir.static.Statics with pcc.ir.static.Aliases {
  type App = pcc.App
  type State = pcc.core.State
  type SrcCtx = pcc.core.SrcCtx
  def SrcCtx = pcc.core.SrcCtx

  implicit def ctx: SrcCtx = SrcCtx.empty

}

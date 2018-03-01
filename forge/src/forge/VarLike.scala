package forge

trait VarLike[T] {
  def __read(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): T
  def __assign(v: T)(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): Unit
}

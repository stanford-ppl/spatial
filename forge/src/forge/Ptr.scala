package forge

class Ptr[T](var value: T) extends VarLike[T] {
  override def __read(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): T = value
  override def __assign(v: T)(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): Unit = { value = v }
}

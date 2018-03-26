package argon

import forge.tags._
import forge.{VarLike,AppState}

trait StagedVarLike[A] extends VarLike[A] {

  @rig def __sread(): A
  @rig def __sassign(v: A): Unit

  def __read(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): A = {
    if ((state eq null) || !state.isInstanceOf[State]) {
      throw new Exception("Attempted to read staged var outside of staged scope.")
    }
    else {
      __sread()(ctx, state.asInstanceOf[State])
    }
  }
  def __assign(v: A)(implicit ctx: SrcCtx = SrcCtx.empty, state: AppState = null): Unit = {
    if ((state eq null) || !state.isInstanceOf[State]) {
      throw new Exception("Attempted to write staged var outside of staged scope.")
    }
    else {
      __sassign(v)(ctx, state.asInstanceOf[State])
    }
  }

}

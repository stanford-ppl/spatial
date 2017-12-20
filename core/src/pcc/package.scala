
package object pcc extends core.Printing with core.Staging with core.Scoping  {
  type State = core.State
  type Config = core.Config

  type SrcCtx = core.SrcCtx

  /** IR **/
  type Op[T] = core.Op[T]
  type Sym[T] = core.Sym[T]
  type Block[T] = core.Block[T]

  def syms(a: Any*): Seq[Sym[_]] = core.Filters.syms(a:_*)

  /** Metadata **/
  type Metadata[T] = core.Metadata[T]
  type SimpleData[T] = core.SimpleData[T]
  type ComplexData[T] = core.ComplexData[T]

  /** Errors **/
  type PlacerError = core.PlacerError
  type SearchFailure = core.SearchFailure
  type TestbenchFailure = core.TestbenchFailure
}

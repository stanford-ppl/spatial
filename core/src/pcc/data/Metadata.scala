package pcc.data

import forge._
import pcc.core._
import pcc.traversal.transform.Transformer

/** Shortcuts for metadata **/
@data object metadata {
  def apply[M<:Metadata[M]:Manifest](edge: Sym[_]): Option[M] = state.metadata[M](edge)
  def add[M<:Metadata[M]:Manifest](edge: Sym[_], m: M): Unit = state.metadata.add[M](edge, m)
  def all(edge: Sym[_]): Iterable[(Class[_],Metadata[_])] = state.metadata.all(edge)
  def addAll(edge: Sym[_], data: Iterable[Metadata[_]]): Unit = state.metadata.addAll(edge,data)
  def addOrRemoveAll(edge: Sym[_], data: Iterable[(Class[_],Option[Metadata[_]])]): Unit = state.metadata.addOrRemoveAll(edge,data)
  def clear[M<:Metadata[M]:Manifest]: Unit = state.metadata.clear[M]
  def clearBeforeTransform(): Unit = state.metadata.clearBeforeTransform()
}

@data object globals {
  def add[M<:Metadata[M]:Manifest](m: M): Unit = state.globals.add[M](m)
  def apply[M<:Metadata[M]:Manifest]: Option[M] = state.globals[M]
  def clear[M<:Metadata[M]:Manifest]: Unit = state.globals.clear[M]
  def mirrorAfterTransform(f: Transformer): Unit = state.globals.mirrorAfterTransform(f)
  def clearBeforeTransform(): Unit = state.globals.clearBeforeTransform()

  def foreach(func: (Class[_],Metadata[_]) => Unit): Unit = state.globals.foreach(func)
}

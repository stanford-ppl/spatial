package forge

import scala.collection.mutable

object recursive {
  private def recurseVisit[T](x: Any)(func: PartialFunction[Any,Unit]): Unit = x match {
    case e if func.isDefinedAt(e) => func(e)
    case ss:Iterator[_] => while (ss.hasNext) { recurseVisit(ss.next)(func) }
    case ss:Iterable[_] => recurseVisit(ss.iterator)(func)
    case ss:Product => recurseVisit(ss.productIterator)(func)
    case _ =>
  }
  def collectSeq[T](blk: PartialFunction[Any, T])(x: Any): Seq[T] = {
    val out = new mutable.ListBuffer[T]
    recurseVisit(x)(blk andThen(e => out += e))
    out.result
  }
  def collectSet[T](blk: PartialFunction[Any, T])(x:Any):Set[T] = {
    val out = new mutable.SetBuilder[T, Set[T]](Set.empty[T])
    recurseVisit(x)(blk andThen(e => out += e))
    out.result
  }
  def collectSeqs[T](blk: PartialFunction[Any, Iterable[T]])(x: Any): Seq[T] = {
    val out = new mutable.ListBuffer[T]
    recurseVisit(x)(blk andThen(e => out ++= e))
    out.result
  }
  def collectSets[T](blk: PartialFunction[Any, Iterable[T]])(x: Any): Set[T] = {
    val out = new mutable.SetBuilder[T, Set[T]](Set.empty[T])
    recurseVisit(x)(blk andThen(e => out ++= e))
    out.result
  }
}

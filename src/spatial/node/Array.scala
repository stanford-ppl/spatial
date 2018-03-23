package spatial.node

import core._
import forge.tags._
import spatial.lang._
import spatial.lang.host._

@op case class InputArguments() extends Op[Array[Text]]

@op case class ArrayNew[A:Type](size: I32) extends Op2[A,Array[A]]

@op case class ArrayFromSeq[A:Type](seq: Seq[Sym[A]]) extends Op2[A,Array[A]]

@op case class ArrayLength[A:Type](array: Array[A]) extends Op[I32]

@op case class ArrayApply[A:Type](coll: Array[A], i: I32) extends Op[A] with AtomicRead[Array[A]] {
  override def aliases = Nil
}

@op case class ArrayUpdate[A:Type](array: Array[A], i: I32, data: Sym[A]) extends Op[Void] {
  override def contains = syms(data)
}

@op case class MapIndices[A:Type](
    size: I32,
    func: Lambda1[I32,A])
  extends Op2[A,Array[A]]

@op case class ArrayForeach[A:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,Void])
  extends Op2[A,Void]

@op case class ArrayMap[A:Type,B:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,B])
  extends Op3[A,B,Array[B]] {
  override def aliases = Nil
}

@op case class ArrayZip[A:Type,B:Type,C:Type](
    arrayA: Array[A],
    arrayB: Array[B],
    applyA: Lambda2[Array[A],I32,A],
    applyB: Lambda2[Array[B],I32,B],
    func:   Lambda2[A,B,C])
  extends Op4[A,B,C,Array[C]] {
  override def aliases = Nil
}

@op case class ArrayReduce[A:Type](
    array:  Array[A],
    apply:  Lambda2[Array[A],I32,A],
    reduce: Lambda2[A,A,A])
  extends Op[A]

@op case class ArrayFold[A:Type](
    array:  Array[A],
    init:   Sym[A],
    apply:  Lambda2[Array[A],I32,A],
    reduce: Lambda2[A,A,A])
  extends Op[A]

@op case class ArrayFilter[A:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    cond:  Lambda1[A,Bit])
  extends Op2[A,Array[A]] {
  override def aliases = Nil
}

@op case class ArrayFlatMap[A:Type,B:Type](
    array: Array[A],
    apply: Lambda2[Array[A],I32,A],
    func:  Lambda1[A,Array[B]])
  extends Op3[A,B,Array[B]] {
  override def aliases = Nil
}

@op case class ArrayMkString[A:Type](
    array: Array[A],
    start: Text,
    delim: Text,
    end:   Text)
  extends Op2[A,Text]


package spatial.lang
package host

import core._
import forge.tags._

/** A one-dimensional array on the host. */
@ref class Array[A:Type] extends Ref[scala.Array[Any],Array[A]] {
  override val __isPrimitive = false


  /** Returns the size of this Array. **/
  @api def length: I32 = wrap{ Array.length(this.s) }

  /** Returns the element at index `i`. **/
  @api def apply(i: I32): T = wrap{ Array.apply(this.s, i.s) }
  /** Updates the element at index `i` to data. **/
  @api def update[A](i: I32, data: A)(implicit lift: Lift[A,T]): MUnit = Unit(Array.update(this.s,i.s,lift(data).s))

  /** Applies the function **func** on each element in the Array. **/
  @api def foreach(func: T => MUnit): MUnit
  = Unit(Array.foreach(this.s, {t: Exp[T] => func(wrap(t)).s}, fresh[I32]))
  /** Returns a new Array created using the mapping `func` over each element in this Array. **/
  @api def map[R:Type](func: T => R): Array[R]
  = Array(Array.map(this.s, {t: Exp[T] => func(wrap(t)).s}, fresh[I32]))
  /** Returns a new Array created using the pairwise mapping `func` over each element in this Array
    * and the corresponding element in `that`.
    */
  @api def zip[S:Type,R:Type](that: Array[S])(func: (T,S) => R): Array[R]
  = Array(Array.zip(this.s, that.s, {(a:Exp[T],b:Exp[S]) => func(wrap(a), wrap(b)).s }, fresh[I32]))
  /** Reduces the elements in this Array into a single element using associative function `rfunc`. **/
  @api def reduce(rfunc: (T,T) => T): T
  = wrap{ Array.reduce(this.s,{(a:Exp[T],b:Exp[T]) => rfunc(wrap(a),wrap(b)).s}, fresh[I32], (fresh[T],fresh[T])) }

  /**
    * Reduces the elements in this Array and the given initial value into a single element
    * using associative function `rfunc`.
    */
  @api def fold(init: T)(rfunc: (T,T) => T): T
  = wrap{ Array.fold(this.s, init.s, {(a:Exp[T],b:Exp[T]) => rfunc(wrap(a),wrap(b)).s}, fresh[I32], (fresh[T],fresh[T])) }
  /** Returns a new Array with all elements in this Array which satisfy the given predicate `cond`. **/
  @api def filter(cond: T => MBoolean): Array[T]
  = Array(Array.filter(this.s, {t:Exp[T] => cond(wrap(t)).s}, fresh[I32]))
  /** Returns a new Array created by concatenating the results of `func` applied to all elements in this Array. **/
  @api def flatMap[R:Type](func: T => Array[R]): Array[R]
  = Array(Array.flatmap(this.s,{t:Exp[T] => func(wrap(t)).s}, fresh[I32]))

  /** Creates a string representation of this Array using the given `delimeter`. **/
  @api def mkString(delimeter: MString): MString = this.mkString("", delimeter, "")

  /** Creates a string representation of this Array using the given `delimeter`, bracketed by `start` and `stop`. **/
  @api def mkString(start: MString, delimeter: MString, stop: MString): MString
  = wrap(Array.string_mk(this.s, start.s, delimeter.s, stop.s))

  /** Returns true if this Array and `that` contain the same elements, false otherwise. **/
  @api def ===(that: Array[T]): MBoolean = this.zip(that){(x,y) => x === y }.reduce{_ && _}
  /** Returns true if this Array and `that` differ by at least one element, false otherwise. **/
  @api def =!=(that: Array[T]): MBoolean = this.zip(that){(x,y) => x =!= y }.reduce{_ || _}

  @api def toText: MString = String.ify(this)
}

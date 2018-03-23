package models

import scala.language.higherKinds

case class Model[T,F[A]<:Fields[A,F]](name: String, params: Seq[String], entries: Map[String,T])(implicit val config: F[T]) {
  def fullName: String = name + "_" + params.mkString("_")
  private val fields: Array[String] = config.fields
  private val default: T = config.default
  def keys: Array[String] = fields
  def nonZeroFields: Array[String] = fields.filter{f => entries.contains(f) && entries(f) != default }
  // HACK - get parameter which gives number
  def n: Option[Int] = {
    val i = params.lastIndexWhere(_ != "")
    if (i >= 0) {
      val x = params(i)
      if (x.nonEmpty && x.forall(_.isDigit)) Some(x.toInt) else None
    }
    else None
  }
  def renameEntries(remapping: String => String): Model[T,F] = {
    val entries2 = entries.map{case (k,v) => remapping(k) -> v }
    new Model[T,F](name, params, entries2)
  }

  def toSeq: Seq[T] = fields.map{f => this.apply(f) }
  def apply(field: String): T = entries.getOrElse(field, default)
  def seq(keys: String*): Seq[T] = keys.map{k => this(k)}

  def foreach(func: (String,T) => Unit): Unit = entries.foreach{case (k,v) => func(k,v) }
  def map[R](func: T => R)(implicit config: F[R]): Model[R,F] = {
    new Model[R,F](name, params, entries.map{case (k,v) => k -> func(v)})
  }
  def zip[S,R](that: Model[S,F])(func: (T,S) => R)(implicit config: F[R]): Model[R,F] = {
    new Model[R,F](name, params, fields.map{k => k -> func(this(k), that(k)) }.toMap)
  }
  def zipExists(that: Model[T,F])(func: (T,T) => Boolean): Boolean = fields.exists{k => func(this(k), that(k)) }
  def zipForall(that: Model[T,F])(func: (T,T) => Boolean): Boolean = fields.forall{k => func(this(k), that(k)) }

  def +(that: Model[T,F])(implicit num: AffArith[T]): Model[T,F] = this.zip(that){(a,b) => num.plus(a,b) }
  def -(that: Model[T,F])(implicit num: AffArith[T]): Model[T,F] = this.zip(that){(a,b) => num.minus(a,b) }
  def /(that: Model[Double,F])(implicit num: AffArith[T]): Model[T,F] = this.zip(that){(a,b) => num.div(a,b) }
  def *(b: Double)(implicit num: AffArith[T]): Model[T,F] = this.map{x => num.times(x,b) }
  def /(b: Double)(implicit num: AffArith[T]): Model[T,F] = this.map{x => num.div(x,b) }

  def isNonZero(implicit num: Numeric[T], ord: Ordering[T]): Boolean = this.toSeq.exists{x => ord.gt(x, num.fromInt(0)) }

  def <(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipForall(that){(a,b) => ord.lt(a,b) }   // a0 < b0 && ... && aN < bN
  def <=(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipForall(that){(a,b) => ord.lteq(a,b) } // a0 <= b0 && ... && aN <= bN
  // These may seem a bit odd, but required to have the property !(a < b) = a >= b
  def >(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipExists(that){(a,b) => ord.gt(a,b) }   // a0 > b0 || ... || aN > b0
  def >=(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipExists(that){(a,b) => ord.gteq(a,b) } // a0 >= b0 || ... || aN >= b0

  // Alternative comparisons, where < is true if any is less than, > is true iff all are greater
  def <<(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipExists(that){(a,b) => ord.lt(a,b) }
  def <<=(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipExists(that){(a,b) => ord.lteq(a,b) }
  def >>(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipForall(that){(a,b) => ord.gt(a,b) }
  def >>=(that: Model[T,F])(implicit ord: Ordering[T]): Boolean = this.zipForall(that){(a,b) => ord.gteq(a,b) }

  override def toString: String = {
    name + fields.map{f => f -> this(f) }
      .filterNot(_._2 == default)
      .map{case (f,v) => s"$f=$v"}
      .mkString("(", ", ", ")")
  }
  def toPrintableString(nParams: Int): String = {
    val padParams = Array.fill(nParams - params.length)("")
    val seq = this.toSeq
    (Array(name) ++ params ++ padParams ++ seq).mkString(",")
  }
}

object Model {
  def zero[T,C[A]<:Fields[A,C]](implicit config: C[T]): Model[T,C] = new Model[T,C]("", Nil, Map.empty)
  def apply[T,C[A]<:Fields[A,C]](entries: (String,T)*)(implicit config: C[T]): Model[T,C] = new Model[T,C]("", Nil, entries.toMap)

  def fromArray[T,C[A]<:Fields[A,C]](name: String, params: Seq[String], entries: Array[T])(implicit config: C[T]): Model[T,C] = {
    new Model[T,C](name, params, config.fields.zip(entries).toMap)
  }
}
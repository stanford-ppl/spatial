import scala.annotation.unchecked.{uncheckedVariance => uV}
import scala.reflect.{ClassTag,classTag}


abstract class Op[+R]

sealed abstract class Def[+C,+R] {
  def getConst: Option[C] = None
  def getOp: Option[Op[R]] = None
}
object Def {
  case object TypeRef extends Def[Nothing,Nothing]

  case class Bound[R](id: Int) extends Def[Nothing,R]
  case class Node[R](id: Int, op: Op[R]) extends Def[Nothing,R] {
    override def getOp: Option[Op[R]] = Some(op)
  }
  case class Const[C](c: C) extends Def[C,Nothing] {
    override def getConst: Option[C] = Some(c)
  }
  case class Param[C](id: Int, c: C) extends Def[C,Nothing] {
    override def getConst: Option[C] = Some(c)
  }
  case class Error[R](id: Int) extends Def[Nothing,R]
}


abstract class ExpType[+C,A:ClassTag](implicit val ev: A <:< Exp[C,A]) {
  type I = C@uV
  def fresh: A
  def typeArgs: List[Type[_]] = Nil
  def supertypes: List[Type[_]] = Nil
  final def _new(d: Def[C@uV,A@uV]): A = { val v = fresh; v.tp = this; v.rhs = d; v }

  def typePrefix: String = classTag[A].runtimeClass.getName.split('$').last
  def typeName: String = typePrefix + (if (typeArgs.isEmpty) "" else "[" + typeArgs.mkString(",") + "]")

  final def getView[B[_]](implicit ev: ClassTag[B[_]]): Option[B[A@uV]] = {
    if (isSubtype(this.getClass,ev.runtimeClass)) Some(this.asInstanceOf[B[A]]) else None
  }

  private def isSubtype(x: java.lang.Class[_], cls: java.lang.Class[_]): Boolean = {
    if ((x == cls) || x.getInterfaces.contains(cls)) true
    else if (x.getSuperclass == null && x.getInterfaces.length == 0) false
    else {
      val superIsSub = if (x.getSuperclass != null) isSubtype(x.getSuperclass, cls) else false
      superIsSub || x.getInterfaces.exists(s=>isSubtype(s,cls))
    }
  }

  final def <:<(that: ExpType[_,_]): Boolean = this <:> that || supertypes.exists{s => s <:< that }
  final def <:>(that: ExpType[_,_]): Boolean = {
    this.typePrefix == that.typePrefix && this.typeArgs == that.typeArgs
  }
}
type Type[A] = ExpType[_,A]
object Type {
  def apply[A:Type]: Type[A] = implicitly[Type[A]]
}

trait Exp[+C,+A] extends Serializable with Equals {
  type I = C@uV
  var _tp: ExpType[C@uV,A@uV] = _
  def tp_=(tp: ExpType[C@uV,A@uV]): Unit = { _tp = tp }
  def tp: ExpType[C@uV,A@uV] = _tp

  var _rhs: Def[C@uV,A@uV] = _
  def rhs_=(rhs: Def[C@uV,A@uV]): Unit = { _rhs = rhs }
  def rhs: Def[C@uV,A@uV] = _rhs
  def op: Option[Op[A@uV]] = rhs.getOp
  def c: Option[I] = rhs.getConst

  def asType: A = { _rhs = Def.TypeRef; this.asInstanceOf[A] }
}
type Sym[A] = Exp[_,A]

trait Ref[+C,+A] extends ExpType[C,A@uV] with Exp[C,A] {
  override type I = C@uV

  final override def hashCode(): Int = this.rhs match {
    case Def.Const(c)    => c.hashCode()
    case Def.Param(id,_) => id
    case Def.Node(id,_)  => id
    case Def.Bound(id)   => id
    case Def.Error(id)   => id
    case Def.TypeRef     => (typePrefix,typeArgs).hashCode()
  }

  final override def canEqual(that: Any): Boolean = that.isInstanceOf[Exp[_,_]]

  final override def equals(x: Any): Boolean = x match {
    case that: Ref[_,_] => (this.rhs, that.rhs) match {
      case (Def.Const(a),     Def.Const(b))     => this.tp == that.tp && a == b
      case (Def.Param(idA,_), Def.Param(idB,_)) => idA == idB
      case (Def.Node(idA,_),  Def.Node(idB,_))  => idA == idB
      case (Def.Bound(idA),   Def.Bound(idB))   => idA == idB
      case (Def.Error(idA),   Def.Error(idB))   => idA == idB
      case (Def.TypeRef,      Def.TypeRef)      => this <:> that
      case _ => false
    }
    case _ => false
  }

  final override def toString: String = rhs match {
    case Def.Const(c)    => s"'$c'"
    case Def.Param(id,c) => s"p$id ($c)"
    case Def.Node(id,_)  => s"x$id"
    case Def.Bound(id)   => s"b$id"
    case Def.Error(_)    => s"<error>"
    case Def.TypeRef     => s"$typeName"
  }
}


object Staging {

  //implicit def stagedAsSym[A:Type](x: A): A <:< Sym[A] = Type[A].ev
  implicit def unwrap[A:Type](a: A): Sym[A] = Type[A].ev(a)
  //implicit def wrap[A:Type](a: Sym[A]): A = a.asInstanceOf[A]

  def bound[A:Type]: A = Type[A]._new(Def.Bound(0))
  def const[A<:Sym[A]:Type](c: A#I): A = Type[A]._new(Def.Const(c))
  def const[C,A](tp: ExpType[C,A], c: C): A = tp._new(Def.Const(c))
}
import Staging._

object Lit {
  case class LitUn[C]() {
    def unapply(x: Any): Option[C] = x match {
      case r: Ref[_,_] => r.c.map(_.asInstanceOf[C])
      case _ => None
    }
  }

  def apply[C] = LitUn[C]()
  //def unapply[C,A<:Ref[C,A]](x: A): Option[C] = x.c

}


trait Top[A] extends Ref[Nothing,A] {
  def !==(that: Top[A]): Bit = Bit.c(false)
  def ===(that: Top[A]): Bit = Bit.c(true)
}

trait Bits[A] extends Top[A] with Ref[Nothing,A] {
  def nbits: Int
}
object Bits {
  def apply[A:Bits]: Bits[A] = implicitly[Bits[A]]
}

class Vec[A:Bits](length: Int) extends Bits[Vec[A]] with Ref[List[Any],Vec[A]]  {
  val tA: Bits[A] = Bits[A]
  def fresh: Vec[A] = new Vec[A](length)
  def nbits: Int = tA.nbits * length
}
object Vec {
  def tp[A:Bits](len: Int): Vec[A] = new Vec[A](len).asType
}

class Bit extends Bits[Bit] with Ref[Boolean,Bit] {
  def fresh: Bit = new Bit
  def nbits: Int = 1
}
object Bit {
  implicit val tp: Bits[Bit] = (new Bit).asType
  def c(x: Boolean): Bit = const[Bit](x)
}




val x: Bit = bound[Bit]
val BL = Lit[Boolean]

x match {
  case BL(c) => println(c && false)
  case _ => println("None")
}

println(x.tp)

println(Bit.c(true))
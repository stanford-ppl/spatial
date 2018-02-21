package nova.implicits

import forge.tags._
import nova.util.IntLike

object intlike {
  implicit class IntLikeOps[A](a: A) {
    @api def +(b: Int)(implicit int: IntLike[A]): A = int.plus(a,int.fromInt(b))
    @api def -(b: Int)(implicit int: IntLike[A]): A = int.minus(a,int.fromInt(b))
    @api def *(b: Int)(implicit int: IntLike[A]): A = int.times(a,int.fromInt(b))
    @api def /(b: Int)(implicit int: IntLike[A]): A = int.divide(a,int.fromInt(b))
    @api def %(b: Int)(implicit int: IntLike[A]): A = int.modulus(a,int.fromInt(b))
    @api def +(b: A)(implicit int: IntLike[A]): A = int.plus(a,b)
    @api def -(b: A)(implicit int: IntLike[A]): A = int.minus(a,b)
    @api def *(b: A)(implicit int: IntLike[A]): A = int.times(a,b)
    @api def /(b: A)(implicit int: IntLike[A]): A = int.divide(a,b)
    @api def %(b: A)(implicit int: IntLike[A]): A = int.modulus(a,b)
  }

  implicit class IntOps(a: Int) {
    @api def +[A](b: A)(implicit int: IntLike[A]): A = int.plus(int.fromInt(a),b)
    @api def -[A](b: A)(implicit int: IntLike[A]): A = int.minus(int.fromInt(a),b)
    @api def *[A](b: A)(implicit int: IntLike[A]): A = int.times(int.fromInt(a),b)
    @api def /[A](b: A)(implicit int: IntLike[A]): A = int.divide(int.fromInt(a),b)
    @api def %[A](b: A)(implicit int: IntLike[A]): A = int.modulus(int.fromInt(a),b)
  }

  def zeroI[A](implicit int: IntLike[A]): A = int.zero
  def oneI[A](implicit int: IntLike[A]): A = int.one
}

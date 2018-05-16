package spatial.lang
package static

import argon._
import forge.tags._

trait Constructors {

  implicit class Tensor1Constructor[A](domain: Series[A]) {
    @api def apply[R:Type](func: A => R): Tensor1[R] = {
      val len = domain.length
      Tensor1.tabulate(len){i => func(domain.at(i)) }
    }
  }

  implicit class Tensor2Constructor[A,B](domain: (Series[A], Series[B]) ) {
    @api def apply[R:Type](func: (A,B) => R): Tensor2[R] = {
      val rows = domain._1.length
      val cols = domain._2.length
      Tensor2.tabulate(rows, cols){(i,j) => func(domain._1.at(i), domain._2.at(j)) }
    }

    @api def foreach(func: (A,B) => Any): Void = {
      domain._1.foreach{i => domain._2.foreach{j => func(i,j); void }}
    }
  }

  implicit class Tensor3Constructor[A,B,C](ranges: (Series[A], Series[B], Series[C]) ) {
    @api def apply[R:Type](func: (A,B,C) => R): Tensor3[R] = {
      val dim0 = ranges._1.length
      val dim1 = ranges._2.length
      val dim2 = ranges._3.length
      Tensor3.tabulate(dim0,dim1,dim2){(i,j,k) => func(ranges._1.at(i), ranges._2.at(j), ranges._3.at(k)) }
    }

    @api def foreach(func: (A,B,C) => Any): Void = {
      ranges._1.foreach{i => ranges._2.foreach{j => ranges._3.foreach{k => func(i,j,k); void }}}
    }
  }

  implicit class Tensor4Constructor[A,B,C,D](ranges: (Series[A], Series[B], Series[C], Series[D]) ) {
    @api def apply[R:Type](func: (A,B,C,D) => R): Tensor4[R] = {
      val dim0 = ranges._1.length
      val dim1 = ranges._2.length
      val dim2 = ranges._3.length
      val dim3 = ranges._4.length
      Tensor4.tabulate(dim0,dim1,dim2,dim3){(i0,i1,i2,i3) =>
        func(ranges._1.at(i0), ranges._2.at(i1), ranges._3.at(i2), ranges._4.at(i3))
      }
    }

    @api def foreach(func: (A,B,C,D) => Any): Void = {
      ranges._1.foreach{i => ranges._2.foreach{j =>
        ranges._3.foreach{k => ranges._4.foreach{l => func(i,j,k,l); void }}
      }}
    }
  }

  implicit class Tensor5Constructor[A,B,C,D,E](ranges: (Series[A], Series[B], Series[C], Series[D], Series[E]) ) {
    @api def apply[R:Type](func: (A,B,C,D,E) => R): Tensor5[R] = {
      val dim0 = ranges._1.length
      val dim1 = ranges._2.length
      val dim2 = ranges._3.length
      val dim3 = ranges._4.length
      val dim4 = ranges._5.length
      Tensor5.tabulate(dim0,dim1,dim2,dim3,dim4){(i0,i1,i2,i3,i4) =>
        func(ranges._1.at(i0), ranges._2.at(i1), ranges._3.at(i2), ranges._4.at(i3), ranges._5.at(i4))
      }
    }

    @api def foreach(func: (A,B,C,D,E) => Any): Void = {
      ranges._1.foreach{i => ranges._2.foreach{j =>
        ranges._3.foreach{k => ranges._4.foreach{l =>
          ranges._5.foreach{m => func(i,j,k,l,m); void }
        }}
      }}
    }
  }

}

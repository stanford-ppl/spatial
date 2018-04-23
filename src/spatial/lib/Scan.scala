package spatial.lib

import forge.tags._
import spatial.dsl._

trait Scan {

  @virtualize
  @api def filter[T:Num](
    Y: SRAM1[T],          // Output array of matching predicates
    P: T => Boolean,      // Predicate: ("Pass this parameter as a function {e <= (e<2)}") 
    X: SRAM1[T]           // Input Array of Integers
  ): Unit = {
    val size = X.length
    Foreach(0 until size) { i =>
      Y(i) = mux(P(X(i)), 1.to[T], 0.to[T])
    }
  }

  @virtualize
  @api def filter_fifo[T:Num](
    Y: SRAM1[T],          // Output array of matching predicates
    P: T => Boolean,      // Predicate: ("Pass this parameter as a function {e <= (e<2)}") 
    X: SRAM1[T],          // Input Array of Integers
    YS: Reg[Int]
  ): Unit = {
    val size = X.length
    val fifo = FIFO[T](size)
    Foreach(size by 1) { i =>
      if (P(X(i))) {
        fifo.enq(X(i))
      }
    }
    YS := fifo.numel
    Foreach (0 until YS) { k =>
      Y(k) = fifo.deq()
    }
  }

}

package spatial.node

import argon._
import argon.node.Alloc
import forge.tags._
import spatial.lang._

/** An allocation of a subclass C of LockSRAM with elements of type A.
  * @param dims The dimensions of the memory
  */
@op case class LockSRAMNew[A:Bits,C[T]](
    dims: Seq[I32]
    )(implicit val tp: Type[C[A]])
  extends MemAlloc[A,C]

/** Read of a single element from an LockSRAM.
  * @param mem The memory instance being read
  * @param addr The N-dimensional address
  * @param ens Associated read enable(s)
  */
@op case class LockSRAMRead[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    addr: Seq[Idx],
    lock: Option[Lock[I32]],
    ens:  Set[Bit])
  extends Reader[A,A]

/** Write of a single element to an LockSRAM.
  * @param mem The memory instance being written
  * @param data The element being written
  * @param addr The N-dimensional address
  * @param ens Associated write enable(s)
  */
@op case class LockSRAMWrite[A:Bits,C[T]](
    mem:  LockSRAM[A,C],
    data: Bits[A],
    addr: Seq[Idx],
    lock: Option[Lock[I32]],
    ens: Set[Bit])
  extends Writer[A]

/** An allocation of a Lock node.
  * @param depth Width of the locking module
  */
@op case class LockNew[A:Bits](depth: I32)(implicit val tp: Type[A])
  extends MemAlloc[A,Lock] {
    def dims = Seq(depth)
  }

@op case class LockOnKeys[A:Bits](keys: Seq[A])(implicit val tp: Type[A])
    extends Alloc[Lock[A]]
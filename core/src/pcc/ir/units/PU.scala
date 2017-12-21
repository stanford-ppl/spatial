package pcc
package ir
package units

import pcc.ir.memories.Box

abstract class PU[A](eid: Int)(implicit ev: A <:< PU[A]) extends Box[A](eid)

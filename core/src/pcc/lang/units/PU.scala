package pcc.lang
package units

abstract class PU[A](eid: Int)(implicit ev: A <:< PU[A]) extends Box[A](eid)

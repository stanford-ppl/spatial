package pcc

import pcc.ir.static.Statics

package object ir extends Statics {
  type Bits[A] = pcc.ir.typeclasses.Bits[A]
  type Num[A] = pcc.ir.typeclasses.Num[A]
}

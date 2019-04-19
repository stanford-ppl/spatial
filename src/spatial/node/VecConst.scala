package spatial.node

import argon._
import argon.node._
import argon.lang._
import argon.schedule.Schedule
import forge.tags._

@op case class FixVecConstNew[S:BOOL,I:INT,F:INT](vs:Seq[Int]) extends Primitive[Fix[S,I,F]]
@op case class BitVecConstNew(vs:Seq[Boolean]) extends Primitive[Bit]

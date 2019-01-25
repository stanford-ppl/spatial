package fringe.templates.memory

import chisel3._
import chisel3.util.Mux1H
import fringe.templates.math._
import fringe.templates.counters.SingleCounter
import fringe.utils.{log2Up, getRetimed}
import fringe.utils.XMap._
import fringe.utils.DMap._
import fringe.utils.implicits._
import fringe.Ledger
import fringe._

import scala.math.log

object implicits {

}
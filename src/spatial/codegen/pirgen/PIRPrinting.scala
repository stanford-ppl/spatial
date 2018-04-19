package spatial.codegen.pirgen

import argon._
import forge.tags._
import spatial.lang._
import spatial.node._
import spatial.data._

trait PIRPrinting { self:PIRCodegen =>

  def quote(x:Any):String = x match {
    case x:Sym[_] => self.quote(x)
    case x:Iterable[_] => x.map(quote).toString
    case x => s"$x"
  }

  def dbgblk[T](msg:Any)(x: => T): T = if (config.enDbg) {
    dbg(s"$msg {")
    state.logTab += 1
    val res = x
    state.logTab -= 1
    res match {
      case res:Unit =>
      case res:Iterable[_] if res.size >= 5 => 
        dbg(s"$msg:")
        state.logTab += 1
        res.foreach { elem => dbg(s"${quote(elem)}") }
        state.logTab -= 1
      case res => dbg(s"$msg = ${quote(res)}")
    }
    dbg("}")
    res
  } else x

  val times = scala.collection.mutable.Stack[Long]()
  def tic = {
    times.push(System.nanoTime())
  }
  def toc(unit:String):Double = {
    val startTime = times.pop()
    val endTime = System.nanoTime()
    val timeUnit = unit match {
      case "ns" => 1
      case "us" => 1000
      case "ms" => 1000000
      case "s" => 1000000000
      case _ => throw new Exception(s"Unknown time unit!")
    }
    (endTime - startTime) * 1.0 / timeUnit
  }

  def toc(info:String, unit:String):Unit = {
    val time = toc(unit)
    println(s"$info elapsed time: ${f"$time%1.3f"}$unit")
  }
}

package fringe

import scala.collection.mutable._
import chisel3._
import java.io.{File, PrintWriter}


object ModuleParams {

  private val mapping = HashMap[String, Any]()

  def addParams(name: String, payload: Any): Unit = if (!mapping.contains(name)) mapping += (name -> payload)

  def getParams(name: String): Any = mapping(name)

}
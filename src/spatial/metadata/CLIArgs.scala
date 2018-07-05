package spatial.metadata

import argon._
import forge.tags._
import spatial.lang.I32
import spatial.metadata.bounds.Final

/** A map of names for all command line arguments at application runtime.
  *
  * Option:  CLIArgs.get(int | i32)  --- for a single argument index (unstaged | staged)
  * Getter:  CLIArgs.all             --- for entire map
  * Getter:  CLIArgs(int | i32)      --- for a single argument index (unstaged | staged)
  * Setter:  CLIArgs(int) = name
  * Default: empty map               --- for entire map
  * Default: "???"                   --- for a single argument index
  */
case class CLIArgs(map: Map[Int,String]) extends Data[CLIArgs](Transfer.Mirror)

@data object CLIArgs {
  def all: Map[Int,String] = globals[CLIArgs].map(_.map).getOrElse(Map.empty)
  def get(i: Int): Option[String] = all.get(i)
  def get(i: I32): Option[String] = i match {
    case Final(c) => get(c)
    case _ => None
  }

  def apply(i: Int): String = get(i).getOrElse("???")
  def apply(i: I32): String = get(i).getOrElse("???")

  def update(i: Int, name: String): Unit = get(i) match {
    case Some(n) => globals.add(CLIArgs(all + (i -> s"$n / $name")))
    case None    => globals.add(CLIArgs(all + (i -> name)))
  }

  def listNames: Seq[String] = {
    val argInts = all.toSeq.map(_._1)
    if (argInts.nonEmpty) {
      (0 to argInts.max).map { i =>
        CLIArgs.get(i) match {
          case Some(name) => s"<$i: $name>"
          case None       => s"<$i: (no name)>"
        }
      }
    }
    else Seq("<No input args>")
  }
}
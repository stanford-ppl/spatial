package spatial.data

import argon._
import forge.tags._
import spatial.lang._

import scala.collection.mutable.HashMap

/** A list of all Accel scopes in the program
  *
  * Getter: hwScopes.all
  * Default: Nil
  */
case class AccelScopes(scopes: Seq[Controller]) extends GlobalData[AccelScopes]
@data object hwScopes {
  def all: Seq[Controller] = globals[AccelScopes].map(_.scopes).getOrElse(Nil)
}

/** Global set of all local memories.
  *
  * Getter:  localMems.all
  * Append:  localMems += (mem)
  * Default: empty set
  */
case class LocalMemories(memories: Set[Sym[_]]) extends GlobalData[LocalMemories]
@data object localMems {
  def all: Set[Sym[_]] = globals[LocalMemories].map(_.memories).getOrElse(Set.empty)
  def +=(mem: Sym[_]): Unit = globals.add(LocalMemories(localMems.all + mem ))
}


/** A map from ephemeral nodes to all ephemeral nodes they (indirectly) use.
  *
  * Used for usage analysis when removing ephemeral nodes.
  * Getter:  pendingUses.all   --- for entire map
  * Getter:  pendingUses(sym)  --- for an individual symbol
  * Setter:  pendingUses += sym -> uses
  * Reset:   pendingUses.reset()
  * Default: Empty map
  */
case class MPendingUses(nodes: HashMap[Sym[_],Set[Sym[_]]]) extends GlobalData[MPendingUses]
@data object pendingUses {
  def all: HashMap[Sym[_],Set[Sym[_]]] = globals[MPendingUses].map(_.nodes).getOrElse(HashMap.empty)
  def apply(x: Sym[_]): Set[Sym[_]] = pendingUses.all.getOrElse(x, Set.empty)
  def contains(x: Sym[_]): Boolean = pendingUses.all.contains(x)
  def +=(entry: (Sym[_], Set[Sym[_]])): Unit = pendingUses.all += entry
  def reset(): Unit = globals.add(MPendingUses(HashMap.empty))
}


/** A map of names for all command line arguments at application runtime.
  *
  * Option:  CLIArgs.get(int | i32)  --- for a single argument index (unstaged | staged)
  * Getter:  CLIArgs.all             --- for entire map
  * Getter:  CLIArgs(int | i32)      --- for a single argument index (unstaged | staged)
  * Setter:  CLIArgs(int) = name
  * Default: empty map               --- for entire map
  * Default: "???"                   --- for a single argument index
  */
case class CLIArgs(map: Map[Int,String]) extends StableData[CLIArgs]

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


case class StreamLoads(ctrls: Set[Sym[_]]) extends GlobalData[StreamLoads]
@data object streamLoadCtrls {
  def all: Set[Sym[_]] = globals[StreamLoads].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(StreamLoads(streamLoadCtrls.all + ctrl))
}


case class TileTransfers(ctrls: Set[Sym[_]]) extends GlobalData[TileTransfers]
@data object tileTransferCtrls {
  def all: Set[Sym[_]] = globals[TileTransfers].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(TileTransfers(tileTransferCtrls.all + ctrl))
}


case class MStreamEnablers(streams: Set[Sym[_]]) extends GlobalData[MStreamEnablers]
@data object streamEnablers {
  def all: Set[Sym[_]] = globals[MStreamEnablers].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(MStreamEnablers(streamEnablers.all + stream))
}


case class MStreamHolders(streams: Set[Sym[_]]) extends GlobalData[MStreamHolders]
@data object streamHolders {
  def all: Set[Sym[_]] = globals[MStreamHolders].map(_.streams).getOrElse(Set.empty)
  def +=(stream: Sym[_]): Unit = globals.add(MStreamHolders(streamHolders.all + stream))
}


case class MStreamParEnqs(ctrls: Set[Sym[_]]) extends GlobalData[MStreamParEnqs]
@data object streamParEnqs {
  def all: Set[Sym[_]] = globals[MStreamParEnqs].map(_.ctrls).getOrElse(Set.empty)
  def +=(ctrl: Sym[_]): Unit = globals.add(MStreamParEnqs(streamParEnqs.all + ctrl))
}



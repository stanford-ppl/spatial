package spatial.metadata

import argon.{Sym, metadata}

package object transform {
  implicit class TransformOps(s: Sym[_]) {
    def streamify: Boolean = metadata[Streamify](s).exists(_.flag)
    def streamify_=(flag: Boolean): Unit = metadata.add[Streamify](s, Streamify(flag))
    def streamify_=(fields: (Boolean, String)): Unit = metadata.add[Streamify](s, Streamify(fields._1, Some(fields._2)))
  }
}

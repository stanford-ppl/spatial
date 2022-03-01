package utils

trait LowPriorityArgsImplicits { this: Args =>
  implicit def strToArgs(cmd: java.lang.String): Args = Args(Seq(cmd))
}

trait Args extends LowPriorityArgsImplicits {
  val NoArgs: Args = Args(Seq(""))

  case class Args(cmds: Seq[java.lang.String]) {
    // This one is somewhat deceptive -- "and" adds another set of commands to be run in addition to the existing set
    // while + appends its arguments to all existing commands.
    def and(cmd: java.lang.String): Args = Args(cmds :+ cmd)
    def +(cmd: java.lang.String): Args = Args(cmds map {str => s"$str $cmd"})
  }

  implicit class StringArgs(cmd1: java.lang.String) {
    def and(cmd2: java.lang.String): Args = Args(Seq(cmd1, cmd2))
  }
}


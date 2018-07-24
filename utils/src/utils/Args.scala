package utils

trait LowPriorityArgsImplicits { this: Args =>
  implicit def strToArgs(cmd: java.lang.String): Args = Args(Seq(cmd))
}

trait Args extends LowPriorityArgsImplicits {
  val NoArgs: Args = Args(Seq(""))

  case class Args(cmds: Seq[java.lang.String]) {
    def and(cmd: java.lang.String): Args = Args(cmds :+ cmd)
  }

  implicit class StringArgs(cmd1: java.lang.String) {
    def and(cmd2: java.lang.String): Args = Args(Seq(cmd1, cmd2))
  }


}


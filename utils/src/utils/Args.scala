package utils

trait LowPriorityArgsImplicits { this: Args =>
  implicit def strToArgs(cmd: java.lang.String): Args = Args(Seq(cmd))
}

trait Args extends LowPriorityArgsImplicits {

  case class Args(cmds: Seq[java.lang.String]) {
    def and(cmd: java.lang.String): Args = Args(cmds :+ cmd)
  }

  implicit class StringArgs(cmd2: java.lang.String) {
    def and(cmd1: java.lang.String): Args = Args(Seq(cmd1, cmd2))
  }


}


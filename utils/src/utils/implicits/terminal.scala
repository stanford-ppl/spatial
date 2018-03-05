package utils
package implicits

import java.io.PrintStream

object terminal {
  implicit class PrintReport(out: PrintStream) {
    def report(x: => Any): Unit = out.println(x)

    def warn(x: => Any): Unit = out.println(s"[${Console.YELLOW}warn${Console.RESET}] $x")
    def warn(ctx: Ctx, x: => Any): Unit = warn(ctx.toString + ": " + x)
    def warn(ctx: Ctx): Unit = warn(ctx, showCaret = false)
    def warn(ctx: Ctx, showCaret: Boolean): Unit = if (ctx.content.isDefined) {
      warn(ctx.content.get)
      if (showCaret) warn(" "*(ctx.column-1) + "^") else warn("")
    }

    def error(x: => Any): Unit = out.println(s"[${Console.RED}error${Console.RESET}] $x")
    def error(ctx: Ctx, x: => Any): Unit = error(ctx.file + ":" + ctx.line + ": " + x)
    def error(ctx: Ctx): Unit = error(ctx, showCaret = false)
    def error(ctx: Ctx, showCaret: Boolean): Unit = if (ctx.content.isDefined) {
      error(ctx.content.get)
      if (showCaret) error(" "*(ctx.column-1) + "^") else error("")
    }

    def bug(x: => Any): Unit = out.println(s"[${Console.MAGENTA}bug${Console.RESET}] $x")
    def bug(ctx: Ctx, x: => Any): Unit = bug(ctx.file + ":" + ctx.line + ": " + x)
    def bug(ctx: Ctx): Unit = bug(ctx, showCaret = false)
    def bug(ctx: Ctx, showCaret: Boolean): Unit = if (ctx.content.isDefined) {
      bug(ctx.content.get)
      if (showCaret) bug(" "*(ctx.column-1) + "^") else bug("")
    }

    def info(x: => Any): Unit = out.println(s"[${Console.BLUE}info${Console.RESET}] $x")
    def info(ctx: Ctx, x: => Any): Unit = info(ctx.file + ":" + ctx.line + ": " + x)
    def info(ctx: Ctx): Unit = info(ctx, showCaret = false)
    def info(ctx: Ctx, showCaret: Boolean): Unit = if (ctx.content.isDefined) {
      info(ctx.content.get)
      if (showCaret) bug(" "*(ctx.column-1) + "^") else bug("")
    }
  }
}

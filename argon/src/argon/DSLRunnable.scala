package argon

import java.io.File

/** All classes which may contain implicit state for staging programs. */
trait DSLRunnable { self =>

  // TODO[2]: Support multiple compile runs? How to preserve global declarations across compilations?
  final protected[argon] implicit lazy val IR: State = {
    val state = new State(this)
    state.config = initConfig()
    val cwd = new File(".").getAbsolutePath
    state.config.logDir = s"$cwd/logs/testbench/$name/"
    state.config.genDir = s"$cwd/gen/testbench/$name/"
    state.config.repDir = s"$cwd/reports/testbench/$name/"
    state.newScope(motion=false)  // Start a new scope (allows global declarations)
    state
  }

  var name: String = self.getClass.getName.replace("class ", "").replace('.','_').replace("$","")

  /** Override to create a custom Config instance */
  def initConfig(): Config = new Config

}

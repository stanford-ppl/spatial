package argon

import java.io.File

/** All classes which may contain implicit state for staging programs. */
trait DSLRunnable { self =>

  // TODO[2]: Support multiple compile runs? How to preserve global declarations across compilations?
  final protected[argon] implicit lazy val IR: State = {
    val state = new State(this)
    state.config = initConfig()
    val cwd = new File(".").getAbsolutePath
    val name = self.name.replace("_", "/")
    state.config.logDir = s"$cwd/logs/$name/"
    state.config.genDir = s"$cwd/gen/$name/"
    state.config.repDir = s"$cwd/reports/$name/"
    state.config.setV(1)
    state.newScope(motion=false)  // Start a new scope (allows global declarations)
    state
  }

  var name: String = self.getClass.getSimpleName.replace("class ", "").replace("$","")

  /** Override to create a custom Config instance */
  def initConfig(): Config = new Config

}

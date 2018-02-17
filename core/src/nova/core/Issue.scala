package nova.core

import forge.tags._

/**
  * Issues are problems that need resolving in the compiler.
  *
  * Not resolving a raised Issue by the end of the pass after it was raised results in an error.
  */
abstract class Issue { this: Product =>

  /**
    * Method called when this issue remains unresolved.
    */
  @stateful def onUnresolved(traversal: String): Unit = {
    error(s"Issue ${this.productPrefix} was unresolved after traversal $traversal.")
    state.logError()
  }
}

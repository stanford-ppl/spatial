package fringe.templates.math

/** Operation done when Fixed Point operations result in a value outside representable bounds. */
sealed trait OverflowMode
/** Use standard overflow wrapping on overflow or underflow. */
object Wrapping extends OverflowMode
/** Use saturating math (don't wrap). */
object Saturating extends OverflowMode



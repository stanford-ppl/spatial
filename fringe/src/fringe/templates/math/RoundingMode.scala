package fringe.templates.math

/** Fixed point rounding mode. */
sealed trait RoundingMode
/** Drop the least significant bits (always round to negative infinity). */
object Truncate extends RoundingMode
/** Use a LFSR for stochastic (unbiased) rounding. */
object Unbiased extends RoundingMode

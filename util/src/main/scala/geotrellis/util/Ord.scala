package geotrellis.util

// --- //

/** A sum type for type-safe comparison operations. */
sealed trait Ord

case object EQ extends Ord

case object LT extends Ord

case object GT extends Ord

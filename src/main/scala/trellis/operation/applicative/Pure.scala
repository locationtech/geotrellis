package trellis.operation.applicative

import trellis.operation._
import trellis.process._

/**
 * This corresponds to Haskell's "pure" on Functor.
 */
case class Pure[Z:Manifest](z:Z)
extends Op0[Z](() => Result(z))

package geotrellis.logic.applicative

import geotrellis._
import geotrellis.process._

/**
 * This corresponds to Haskell's "pure" on Functor.
 */
case class Pure[Z:Manifest](z:Z) extends Op0[Z]({
  () => Result(z)
})

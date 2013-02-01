package geotrellis.logic.applicative

import geotrellis._

/**
 * This corresponds to Haskell's "fmap" on Functor.
 */
case class Fmap[A, Z:Manifest](a:Op[A])(f:A => Z) extends Op1[A, Z](a)({
  a => Result(f(a))
})

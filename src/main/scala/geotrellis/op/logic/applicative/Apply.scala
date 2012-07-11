package geotrellis.op.logic.applicative

import geotrellis.op._
import geotrellis.process._

/**
 * This corresponds to Haskell's "apply" (<*>) on Functor.
 */
case class Apply[A, Z:Manifest](a:Op[A])(f:Op[A => Z])
extends Op2[A, A => Z, Z](a, f)((a, f) => Result(f(a)))

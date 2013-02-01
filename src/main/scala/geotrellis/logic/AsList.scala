package geotrellis.logic

import geotrellis._

/**
 * Return the result of the input operation as a List.
 */
case class AsList[A](x: Op[A]) extends Op1(x)({
    x => Result(List(x))
})

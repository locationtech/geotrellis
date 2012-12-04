package geotrellis.logic

import geotrellis._

case class AsList[A](x: Op[A]) extends Op1(x)(
    x => Result(List(x)))

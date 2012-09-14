package geotrellis.ExampleTwo

import geotrellis._

import geotrellis.process._

import org.scalatest.FunSpec
import org.scalatest.matchers.MustMatchers

case class Timer[T](f:() => T) {
  val t0 = System.currentTimeMillis()
  val result = f()
  val t1 = System.currentTimeMillis()
  val time = t1 - t0
}

class ExampleTwoSpec extends FunSpec with MustMatchers {}

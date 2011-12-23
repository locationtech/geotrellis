package trellis.ExampleTwo

import trellis.operation._

import trellis.process._

import org.scalatest.Spec
import org.scalatest.matchers.MustMatchers

case class Timer[T](f:() => T) {
  val t0 = System.currentTimeMillis()
  val result = f()
  val t1 = System.currentTimeMillis()
  val time = t1 - t0
}

class ExampleTwoSpec extends Spec with MustMatchers {}

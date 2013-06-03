package geotrellis.logic

import geotrellis._
import geotrellis.process._

object Do {
  /**
   * Invoke a function that takes no arguments.
   * 
   * See Do0.
   */
  def apply[Z](call:() => Z) = Do0(call)

  /**
   * Invoke a function that takes one argument.
   * 
   * See Do1.
   */
  def apply[A,Z](a:Op[A])(call:A => Z) = Do1(a)(call)

  /**
   * Invoke a function that takes two arguments.
   *
   * See Do2.
   */
  def apply[A,B,Z](a:Op[A], b:Op[B])(call:(A,B) => Z) = 
    Do2(a,b)(call)
}

/**
 * Invoke a function that takes no arguments.
 */
case class Do0[Z](call:() => Z) extends Op[Z] {
  def _run(context:Context) = Result(call())
  val nextSteps:Steps = {
    case _ => sys.error("Should not be called.")
  }
}

/**
 * Invoke a function that takes one argument.
 *
 * Functionally speaking: Map an Op[A] into an Op[Z] 
 * using a function from A => Z.
 */
case class Do1[A, Z](a:Op[A])(call:A => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: Nil)
  val nextSteps:Steps = {
    case a :: Nil => Result(call(a.asInstanceOf[A]))
  }
}

/**
 * Invoke a function that takes two arguments.
 *
 * Functionally speaking: Map an Op[A] and Op[B] into an Op[Z] using a 
 * function from (A,B) => Z.
 */
case class Do2[A, B, Z]
(a:Op[A], b:Op[B])(call:(A,B) => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: b :: Nil)
  val nextSteps:Steps = {
    case a :: b :: Nil => Result(call(a.asInstanceOf[A],
                                      b.asInstanceOf[B]))
  }
}

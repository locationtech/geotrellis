package geotrellis.operation.logic

import geotrellis.operation._
import geotrellis.process._


object Do {
  /**
   * Invoke a function that takes one argument.
   * 
   * See Do1.
   */
  def apply[A:Manifest,Z:Manifest](a:Op[A])(call:A => Z) = Do1(a)(call)

  /**
   * Invoke a function that takes two arguments.
   *
   * See Do2.
   */
  def apply[A:Manifest,B:Manifest,Z:Manifest](a:Op[A], b:Op[B])(call:(A,B) => Z) = 
    Do2(a,b)(call)
}


/**
 * Invoke a function that takes one argument.
 *
 * Functionally speaking: Map an Op[A] into an Op[Z] 
 * using a function from A => Z.
 */
case class Do1[A:Manifest, Z:Manifest](a:Op[A])(call:A => Z) extends Op[Z] {
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
case class Do2[A:Manifest, B:Manifest, Z:Manifest]
(a:Op[A], b:Op[B])(call:(A,B) => Z) extends Op[Z] {
  def _run(context:Context) = runAsync(a :: b :: Nil)
  val nextSteps:Steps = {
    case a :: b :: Nil => Result(call(a.asInstanceOf[A],
                                      b.asInstanceOf[B]))
  }
}


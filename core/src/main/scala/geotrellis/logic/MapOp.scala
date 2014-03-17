/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

package geotrellis.logic

import geotrellis._
import geotrellis.process._

object MapOp {
  /**
   * Invoke a function that takes no arguments.
   * 
   * See MapOp0.
   */
  def apply[Z](call:() => Z) = MapOp0(call)

  /**
   * Invoke a function that takes one argument.
   * 
   * See MapOp1.
   */
  def apply[A,Z](a:Op[A])(call:A => Z) = MapOp1(a)(call)

  /**
   * Invoke a function that takes two arguments.
   *
   * See MapOp2.
   */
  def apply[A,B,Z](a:Op[A], b:Op[B])(call:(A,B) => Z) = 
    MapOp2(a,b)(call)
}

/**
 * Invoke a function that takes no arguments.
 */
case class MapOp0[Z](call:() => Z) extends Op[Z] {
  def _run() = Result(call())
  val nextSteps:Steps = {
    case _ => sys.error("Should not be called.")
  }
}

/**
 * Invoke a function that takes one argument.
 *
 * Functionally speaking: MapOp an Op[A] into an Op[Z] 
 * using a function from A => Z.
 */
case class MapOp1[A, Z](a:Op[A])(call:A => Z) extends Op[Z] {
  def _run() = runAsync(a :: Nil)
  val nextSteps:Steps = {
    case a :: Nil => Result(call(a.asInstanceOf[A]))
  }
}

/**
 * Invoke a function that takes two arguments.
 *
 * Functionally speaking: MapOp an Op[A] and Op[B] into an Op[Z] using a 
 * function from (A,B) => Z.
 */
case class MapOp2[A, B, Z]
(a:Op[A], b:Op[B])(call:(A,B) => Z) extends Op[Z] {
  def _run() = runAsync(a :: b :: Nil)
  val nextSteps:Steps = {
    case a :: b :: Nil => Result(call(a.asInstanceOf[A],
                                      b.asInstanceOf[B]))
  }
}

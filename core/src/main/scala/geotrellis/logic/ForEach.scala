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

import scala.{PartialFunction => PF}
import geotrellis._

object ForEach {
  def apply[A, Z:Manifest](a:Op[Array[A]])(f:(A) => Op[Z]) = ForEach1(a)(f)
  def apply[A, B, Z:Manifest](a:Op[Array[A]], b:Op[Array[B]])(f:(A, B) => Op[Z]) = ForEach2(a, b)(f)
  def apply[A, B, C, Z:Manifest](a:Op[Array[A]], b:Op[Array[B]], c:Op[Array[C]])(f:(A, B, C) => Op[Z]) = ForEach3(a, b, c)(f)
}

/**
 * Evaluates the given operation (op) to get an array of A's. Then, applies
 * the given function (f) to each item in the array in. The resulting array of
 * Z's is returned.
 */
case class ForEach1[A, Z:Manifest](op:Op[Array[A]])(f:(A) => Op[Z]) extends Op[Array[Z]] {

  def _run() = runAsync(List(op))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: Nil => {
      step2(as.asInstanceOf[Array[A]])
    }

    case zs:List[_] => Result(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A]) = {
    runAsync(as.toList.map(a => f(a)))
  }
}


/**
 * Evaluates the given operations (opA and opB) to get an array of A's and an
 * array of B's. Then, applies the given function (f) to each (A, B) item in
 * the arrays (pairwise by array index) to get a Z value. The resulting array
 * of Z's is returned.
 */
case class ForEach2[A, B, Z:Manifest](opA:Op[Array[A]], opB:Op[Array[B]])
                                     (f:(A, B) => Op[Z]) extends Op[Array[Z]] {

  def _run() = runAsync(List(opA, opB))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]])
    }

    case zs:List[_] => Result(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B]) = {
    runAsync((0 until as.length).map(i => f(as(i), bs(i))).toList)
  }
}


/**
 * Evaluates the given operations (opA opB, and opC) to get arrays of A's, B's
 * and C's (which should be the same length).
 *
 * Then, applies the given function (f) to each (A, B, C) triple in (grouped by
 * array index) to get a Z value. The resulting array of Z's is returned.
 */
case class ForEach3[A, B, C, Z:Manifest](opA:Op[Array[A]],
                                         opB:Op[Array[B]],
                                         opC:Op[Array[C]])
(f:(A, B, C) => Op[Z]) extends Op[Array[Z]] {

  def _run() = runAsync(List(opA, opB, opC))

  val nextSteps:PF[Any, StepOutput[Array[Z]]] = {
    case (as:Array[_]) :: (bs:Array[_]) :: (cs:Array[_]) :: Nil => {
      step2(as.asInstanceOf[Array[A]],
            bs.asInstanceOf[Array[B]],
            cs.asInstanceOf[Array[C]])
    }

    case zs:List[_] => Result(zs.asInstanceOf[List[Z]].toArray)
  }

  def step2(as:Array[A], bs:Array[B], cs:Array[C]) = {
    runAsync((0 until as.length).toList.map(i => f(as(i), bs(i), cs(i))))
  }
}

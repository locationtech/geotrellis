/*
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
 */

package geotrellis.engine.logic

import geotrellis.engine._

/**
 * Conditionally executes one of two operations; if the Boolean Operation evaluates true, the first
 * Operation executes, otherwise the second Operation executes.
 */
case class If[A <: C,B <: C,C:Manifest](bOp: Op[Boolean], trueOp: Op[A], falseOp: Op[B])extends Op[C] {
  def _run() = runAsync('init :: bOp :: Nil)

  val nextSteps: Steps = {
    case 'init :: (b: Boolean) :: Nil => runAsync(
      List('result, 
           if (b) trueOp else falseOp))
    case 'result :: c :: Nil => Result(c.asInstanceOf[C])
  }
}

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

package geotrellis.logic

import geotrellis._
import geotrellis._
import geotrellis.process._

/**
 * Ensure that the result of the operation will be evaluated.
 * 
 * Some raster operations are lazily evaluated, which means that the
 * operations will defer their execution until the
 * moment where execution is necessary.  This allows, for example,
 * some raster operations to be combined and executed at the same
 * time instead of in sequence. 
 *
 * Force will evaluate a lazily evaluated operation if it has not 
 * yet been evaluated.
 * 
 */
// case class Force[A](op:Op[A]) extends Op[A] {
//   def _run() = runAsync(op :: Nil)
//   val nextSteps:Steps = {
//     case (r:Raster) :: Nil => Result(r.force.asInstanceOf[A])
//     case a :: Nil => Result(a.asInstanceOf[A])
//   }
// }

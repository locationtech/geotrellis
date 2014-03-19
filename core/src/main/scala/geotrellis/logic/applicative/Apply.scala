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

package geotrellis.logic.applicative

import geotrellis._
import geotrellis.process._

/**
 * This corresponds to Haskell's "apply" (<*>) on Functor.
 */
case class Apply[A, Z:Manifest](a:Op[A])(f:Op[A => Z]) extends Op2[A, A => Z, Z](a, f)({
  (a, f) => Result(f(a))
})

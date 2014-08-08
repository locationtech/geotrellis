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

package geotrellis.engine.logic.applicative

import geotrellis.engine._
import language.implicitConversions

/**
 * Some implicit operators to add some syntactic sugar. Example:
 *
 * import geotrellis._
 * import geotrellis.op.applicative.Implicits._
 *
 * val f = (a:Int) => (b:Int) => (c:Int) => a + b * c
 * val op = f <@> 1 <*> 2 <*> 3
 *
 * engine.run(op) // returns 7
 */
object Implicits {
  implicit def applyOperator[A, Z:Manifest](lhs:Op[A => Z]) = new {
    def <*>(rhs:Op[A]) = Apply(rhs)(lhs)
  }

  // This coorrespond's to Haskell's <$> which isn't legal in Scala.
  implicit def fmapOperator[A, Z:Manifest](lhs:Function1[A, Z]) = new {
    def <@>(rhs:Op[A]) = Fmap(rhs)(lhs)
  }
}

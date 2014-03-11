/***
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
 ***/

package geotrellis.rest.op.string

import geotrellis.process._
import geotrellis._

/**
 * Split a string on a comma or other delimiter.
 */
object Split {
  def apply(s:Op[String]) = SplitOnComma(s)
}

/**
 * Split a string on a comma.
 */
case class SplitOnComma(s:Op[String]) extends Op1(s)(s => Result(s split ","))

/**
 * Split a string on an arbitrary delimiter.
 */
case class Split(s:Op[String], delim:Op[String]) extends Op2(s, delim)((s, d) => Result(s split d))


/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.rest.op.string

import geotrellis._

/**
 * Parse a string as an integer.
 */
case class ParseInt(s:Op[String], radix:Int) extends Op1(s)({
  s => Result(Integer.parseInt(s,radix))
})

/**
 * Parse a string as an integer (base 10).
 */
object ParseInt {
  def apply(s:Op[String]):ParseInt = ParseInt(s, 10)
}

/**
 * Parse a string as a hexidecimal integer (base 16).
 */
object ParseHexInt {
  def apply(s:Op[String]):ParseInt = ParseInt(s, 16)
}

/*
 * Copyright 2019 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster

import spire.math.Integral
import spire.implicits._

case class Dimensions[@specialized(Byte, Short, Int, Long) N: Integral](cols: N, rows: N) extends Product2[N, N] with Serializable {
  def _1 = cols
  def _2 = rows
  def size: Long = Integral[N].toType[Long](cols) * Integral[N].toType[Long](rows)
  override def toString = s"${cols}x${rows}"
}

object Dimensions {
  implicit def apply[N: Integral](tup: (N, N)) = new Dimensions(tup._1, tup._2)
}
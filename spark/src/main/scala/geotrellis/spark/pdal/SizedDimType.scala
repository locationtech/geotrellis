/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.pdal

import io.pdal.DimType
import io.pdal.PointLayout

case class SizedDimType(dimType: DimType, size: Long, offset: Long)

object SizedDimType {
  // not to overload apply
  def asArray(layout: PointLayout): Array[SizedDimType] = {
    layout.dimTypes().map { dt =>
      SizedDimType(dt, layout.dimSize(dt), layout.dimOffset(dt))
    }
  }

  def asMap(layout: PointLayout): Map[String, SizedDimType] = {
    layout.dimTypes().map { dt =>
      dt.id -> SizedDimType(dt, layout.dimSize(dt), layout.dimOffset(dt))
    }.toMap
  }
}

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

package geotrellis.spark.op

import geotrellis.raster.op.elevation.Hillshade
import geotrellis.spark._
import geotrellis.spark.rdd.RasterRDD

package object elevation {
  implicit class ElevationRasterRDDExtensions(val rasterRDD: RasterRDD)
      extends ElevationRasterRDDMethods

  implicit class HillshadeRasterRDDTuple(val tuple: Tuple2[RasterRDD, RasterRDD]) {
    def hillshade(azimuth: Double, altitude: Double) = {
      val (aspect, slope) = tuple

      aspect.combineTiles(slope) {
        case (TmsTile(r1, t1), TmsTile(r2, t2)) =>
          TmsTile(r1, Hillshade.indirect(t1, t2, azimuth, altitude))
      }
    }
  }
}

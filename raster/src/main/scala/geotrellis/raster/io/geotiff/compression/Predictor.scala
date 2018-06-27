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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import monocle.syntax.apply._

object Predictor {
  val PREDICTOR_NONE = 1
  val PREDICTOR_HORIZONTAL = 2
  val PREDICTOR_FLOATINGPOINT = 3

  def apply(tiffTags: TiffTags): Predictor = {
    (tiffTags
      &|-> TiffTags._nonBasicTags
      ^|-> NonBasicTags._predictor get
    ) match {
      case None | Some(PREDICTOR_NONE) =>
        new Predictor {
          val code = PREDICTOR_NONE
          val checkEndian = true
          def apply(bytes: Array[Byte], segmentIndex: Int) = bytes
        }
      case Some(PREDICTOR_HORIZONTAL) =>
        HorizontalPredictor(tiffTags)
      case Some(PREDICTOR_FLOATINGPOINT) =>
        FloatingPointPredictor(tiffTags)
      case Some(i) =>
        throw new MalformedGeoTiffException(s"predictor tag $i is not valid (require 1, 2 or 3)")
    }
  }
}

trait Predictor extends Serializable {
  /** True if this predictor needs to check if the endian requires flipping */
  def checkEndian: Boolean
  /** GeoTiff tag value for this predictor */
  def code: Int

  def apply(bytes: Array[Byte], segmentIndex: Int): Array[Byte]
}

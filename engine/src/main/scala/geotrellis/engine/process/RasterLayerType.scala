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

package geotrellis.process

import geotrellis._

abstract class RasterLayerType()

case object ArgFile extends RasterLayerType
case object AsciiFile extends RasterLayerType
case object Tiled extends RasterLayerType
case object ConstantRaster extends RasterLayerType

object RasterLayerType {
  implicit def stringToRasterLayerType(s: String) = {
    s match {
      case "constant" => ConstantRaster
      case "asc" => AsciiFile
      case "arg" => ArgFile
      case "tiled" => Tiled
      case _ => sys.error(s"$s is not a valid raster layer type.")
    }
  }
}

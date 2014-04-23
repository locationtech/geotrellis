/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.tiling

import geotrellis.Extent

case class TileCoord(tx: Long, ty: Long) 

case class TileExtent(xmin: Long, ymin: Long, xmax: Long, ymax: Long) {
  def width = xmax - xmin + 1
  def height = ymax - ymin + 1
} 

// width/height is non-inclusive 
case class PixelExtent(xmin: Long, ymin: Long, xmax: Long, ymax: Long) {
  def width = xmax - xmin
  def height = ymax - ymin
}

case class Pixel(px: Long, py: Long)

object Bounds {
	final val World = Extent(-180, -90, 180, 90)
}

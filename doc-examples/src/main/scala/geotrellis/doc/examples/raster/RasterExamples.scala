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

package geotrellis.doc.examples.raster

object RasterExamples {
  def `Counting points in raster cells (raster to vector operation)`: Unit = {
    import geotrellis.raster._
    import geotrellis.vector._

    def countPoints(points: Seq[Point], rasterExtent: RasterExtent): Tile = {
      val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
      val array = Array.ofDim[Int](cols * rows).fill(0)
      for(point <- points) {
        val x = point.x
        val y = point.y
        if(rasterExtent.extent.intersects(x,y)) {
          val index = rasterExtent.mapXToGrid(x) + rasterExtent.mapYToGrid(y) * cols
          array(index) = array(index) + 1
        }
      }
      IntArrayTile(array, cols, rows)
    }
  }

  def `Counting polygon features in raster cells (rasterize operation)`: Unit = {
    import geotrellis.raster._
    import geotrellis.vector._

    def countPolygon(features: Seq[PolygonFeature[Int]], rasterExtent: RasterExtent): MultibandTile = {
      // Assumes the polygon features have 5 classes

      val (cols, rows) = (rasterExtent.cols, rasterExtent.rows)
      val bands = Array.fill(5) { IntArrayTile.ofDim(cols, rows) }
      for(Feature(polygon, classId) <- features) {
        val targetBand = bands(classId)
        // Use the 'foreach' method exension to rasterize the polygon over the raster extent.
        rasterExtent.foreach(polygon) { (col, row) =>
          targetBand.set(col, row, targetBand.get(col, row) + 1)
        }
      }

      MultibandTile(bands)
    }
  }
}

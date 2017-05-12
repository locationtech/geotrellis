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

package geotrellis.raster.distance

import geotrellis.raster._
import geotrellis.raster.rasterize._
import geotrellis.raster.render._
import geotrellis.raster.testkit._
import geotrellis.vector._
import scala.math.{sqrt, pow}

import org.scalatest._

class EuclideanDistanceTileSpec extends FunSpec with Matchers with RasterMatchers {
  describe("Euclidean Distance Tiles") {
    it("should produce the same result as using explicit nearest neighbor search") {
      val pts = Array(Point(0,0), Point(3,1), Point(7,5), Point(4,7), Point(8,3), Point(1,3))
      val rasterExtent = RasterExtent(Extent(0, 0, 8, 8), 512, 512)

      val euclideanDistanceTile = pts.euclideanDistanceTile(rasterExtent)

      def pointToTup(pt: Point): (Double, Double) = (pt.x, pt.y)
      val index = SpatialIndex(pts)(pointToTup(_))
      def smallestDistanceToPoint(tup: (Double, Double)): Double = {
        val nearest = index.nearest(tup)
        sqrt(pow(tup._1 - nearest.x, 2) + pow(tup._2 - nearest.y, 2))
      }

      val tile = DoubleArrayTile.empty(512, 512)
      for (col <- 0 until 512;
           row <- 0 until 512)
        tile.setDouble(col, row, smallestDistanceToPoint(rasterExtent.gridToMap(col, row)))

      // val cm = ColorMap((0.0 to 4.5 by 0.01).toArray, ColorRamps.BlueToRed)
      // euclideanDistanceTile.renderPng(cm).write("distance.png")

      assertEqual(tile, euclideanDistanceTile)
    }
  }
}

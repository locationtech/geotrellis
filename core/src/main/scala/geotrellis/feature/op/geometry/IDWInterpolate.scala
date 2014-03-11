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

package geotrellis.feature.op.geometry

import geotrellis._
import geotrellis.feature._
import geotrellis.raster._

import scalaxy.loops._

/**
 * IDW Interpolation
 */
case class IDWInterpolate(points:Op[Seq[Point[Int]]],re:Op[RasterExtent],radius:Op[Option[Int]]=None)
    extends Op3(points,re,radius)({
  (points,re,radius) =>
    val cols = re.cols
    val rows = re.rows
    val data = RasterData.emptyByType(TypeInt, cols, rows)
    if(points.isEmpty) {
      Result(Raster(data,re))
    } else {



      val r = radius match {
        case Some(r) =>
          val rr = r*r
          val index:SpatialIndex[Point[Int]] = SpatialIndex(points)(p => (p.x,p.y))
          for(col <- 0 until cols optimized) {
            for(row <- 0 until rows optimized) {
              val destX = re.gridColToMap(col)
              val destY = re.gridRowToMap(row)
              val pts = index.pointsInExtent(Extent(destX - r, destY - r, destX + r, destY + r))

              if (pts.isEmpty) {
                data.set(col, row, NODATA)
              } else {
                var s = 0.0
                var c = 0
                var ws = 0.0
                val length = pts.length

                for(i <- 0 until length optimized) {
                  val point = pts(i)
                  val dX = (destX - point.x)
                  val dY = (destY - point.y)
                  val d = dX * dX + dY * dY
                  if (d < rr) {
                    val w = 1 / d
                    s += point.data * w
                    ws += w
                    c += 1
                  }
                }

                if (c == 0) {
                  data.set(col, row, NODATA)
                } else {
                  val mean = s / ws
                  data.set(col, row, mean.toInt)
                }
              }
            }
          }
        case None =>
          val length = points.length
          for(col <- 0 until cols optimized) {
            for(row <- 0 until rows optimized) {
              val destX = re.gridColToMap(col)
              val destY = re.gridRowToMap(row)
              var s = 0.0
              var c = 0
              var ws = 0.0

              for(i <- 0 until length optimized) {
                val point = points(i)
                val dX = (destX - point.x)
                val dY = (destY - point.y)
                val d = dX * dX + dY * dY
                val w = 1 / d
                s += point.data * w
                ws += w
                c += 1
              }

              if (c == 0) {
                data.set(col, row, NODATA)
              } else {
                val mean = s / ws
                data.set(col, row, mean.toInt)
              }
            }
          }
      }
      Result(Raster(data,re))
    }
})

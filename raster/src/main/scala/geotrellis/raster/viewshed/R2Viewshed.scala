/*
 * Copyright 2017 Azavea
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

package geotrellis.raster.viewshed

import geotrellis.raster._
import geotrellis.raster.rasterize.Rasterizer
import geotrellis.vector._

import java.util.Arrays.binarySearch
import java.util.Comparator


object R2Viewshed extends Serializable {

  sealed abstract class From()
  object FromNorth extends From
  object FromEast extends From
  object FromSouth extends From
  object FromWest extends From
  object FromInside extends From


  sealed case class DirectedSegment(
    startCol: Int, startRow: Int,
    endCol: Int, endRow: Int,
    m: Double
  )

  sealed case class Ray(m: Double, alpha: Double)

  type EdgeCallback = (Ray => Unit)

  object RayComparator extends Comparator[Ray] {
    def compare(left: Ray, right: Ray): Int =
      if (left.m < right.m) -1
      else if (left.m > right.m) +1
      else 0
  }

  def apply(tile: Tile, col: Int, row: Int): Tile = {
    val cols = tile.cols
    val rows = tile.rows
    val viewshedTile = ArrayTile.empty(IntCellType, cols, rows)

    R2Viewshed.compute(
      tile, viewshedTile,
      col, row, 0, 1.0,
      FromInside, null, null,
      { _ => }
    )
    viewshedTile
  }

  def compute(
    elevationTile: Tile, viewshedTile: MutableArrayTile,
    startCol: Int, startRow: Int, height: Double, resolution: Double,
    from: From, left: Array[Ray], right: Array[Ray],
    edgeCallback: EdgeCallback
  ): Tile = {
    val cols = elevationTile.cols
    val rows = elevationTile.rows
    val re = RasterExtent(Extent(0, 0, cols, rows), cols, rows)
    val inTile: Boolean = (0 <= startCol && startCol < cols && 0 <= startRow && startRow <= rows)
    val viewHeight =
      if (inTile) elevationTile.getDouble(startCol, startRow) + height
      else height
    var m: Double = 0.0
    var alpha: Double = 0.0

    def clipRayNorthSouth(newStartRow: Int)(endCol: Int, endRow: Int): Option[DirectedSegment] = {
      if (startCol == endCol) {
        if (newStartRow == endRow) None
        else if (startRow >= endRow)
          Some(DirectedSegment(startCol, newStartRow, endCol, endRow, Double.NegativeInfinity))
        else
          Some(DirectedSegment(startCol, newStartRow, endCol, endRow, Double.PositiveInfinity))
      }
      else {
        val m = (startRow - endRow).toDouble / (startCol - endCol)
        val newStartCol = math.round(((newStartRow - startRow) / m) + startCol).toInt
        if (newStartCol == endCol && newStartRow == endRow) None
        else if (0 <= newStartCol && newStartCol < cols)
          Some(DirectedSegment(newStartCol, newStartRow, endCol, endRow, m))
        else None
      }
    }

    def clipRayEastWest(newStartCol: Int)(endCol: Int, endRow: Int): Option[DirectedSegment] = {
      if (startCol == endCol) None
      else {
        val m = (startRow - endRow).toDouble / (startCol - endCol)
        val newStartRow = math.round(m * (newStartCol - startCol) + startRow).toInt
        if (newStartCol == endCol && newStartRow == endRow) None
        else if (0 <= newStartRow && newStartRow < rows)
          Some(DirectedSegment(newStartCol, newStartRow, endCol, endRow, m))
        else None
      }
    }

    def clipRayInside(endCol: Int, endRow: Int): Option[DirectedSegment] = {
      val m = (startRow - endRow).toDouble / (startCol - endCol)
      Some(DirectedSegment(startCol, startRow, endCol, endRow, m))
    }

    val clipRay: ((Int, Int) => Option[DirectedSegment]) =
      from match {
        case FromNorth => clipRayNorthSouth(0)
        case FromEast => clipRayEastWest(cols-1)
        case FromSouth => clipRayNorthSouth(rows-1)
        case FromWest => clipRayEastWest(0)
        case FromInside =>
          if (inTile) clipRayInside
          else throw new Exception("Cannot be both inside and outside")
      }

    val slopeToAlpha: (Double => Double) =
      from match {
        case FromInside => { _ => -Math.PI }
        case _ => { m: Double =>
          val array = if (m < 0) left; else right
          val index = binarySearch(array, Ray(math.abs(m), Double.NaN), RayComparator)

          if (index >= 0) array(index).alpha
          else {
            val place = -1 - index
            if (place == array.length) array.last.alpha
            else array(place).alpha // XXX interpolate
          }
        }
      }

    def callback(col: Int, row: Int) = {
      if (col == startCol && row == startRow) { // starting point
        viewshedTile.setDouble(col, row, 1)
      }
      else { // any other point
        val deltax = startCol-col
        val deltay = startRow-row
        val distance = math.sqrt(deltax*deltax + deltay*deltay) * resolution
        val angle = math.atan((elevationTile.getDouble(col, row) - viewHeight) / distance)

        if (alpha <= angle) {
          alpha = angle
          viewshedTile.setDouble(col, row, 1)
        }
      }
    }

    Range(0, cols) // North
      .flatMap({ col => clipRay(col, 0) })
      .foreach({ seg =>
        m = seg.m
        alpha = slopeToAlpha(m)
        // println(s"NORTH ($startCol, $startRow) $seg $alpha")
        Rasterizer.foreachCellInGridLine(
          seg.startCol, seg.startRow,
          seg.endCol, seg.endRow,
          null, re, false
        )(callback)
        edgeCallback(Ray(m, alpha))
      })
    Range(0, rows) // East
      .flatMap({ row => clipRay(cols-1, row) })
      .foreach({ seg =>
        m = seg.m
        alpha = slopeToAlpha(m)
        // println(s"EAST ($startCol, $startRow) $seg $alpha")
        Rasterizer.foreachCellInGridLine(
          seg.startCol, seg.startRow,
          seg.endCol, seg.endRow,
          null, re, false
        )(callback)
        edgeCallback(Ray(m, alpha))
      })
    Range(0, cols) // South
      .flatMap({ col => clipRay(col, rows-1) })
      .foreach({ seg =>
        m = seg.m
        alpha = slopeToAlpha(m)
        // println(s"SOUTH ($startCol, $startRow) $seg $alpha")
        Rasterizer.foreachCellInGridLine(
          seg.startCol, seg.startRow,
          seg.endCol, seg.endRow,
          null, re, false
        )(callback)
        edgeCallback(Ray(m, alpha))
      })
    Range(0, rows) // West
      .flatMap({ row => clipRay(0, row) })
      .foreach({ seg =>
        m = seg.m
        alpha = slopeToAlpha(m)
        // println(s"WEST ($startCol, $startRow) $seg $alpha")
        Rasterizer.foreachCellInGridLine(
          seg.startCol, seg.startRow,
          seg.endCol, seg.endRow,
          null, re, false
        )(callback)
        edgeCallback(Ray(m, alpha))
      })

    viewshedTile
  }

}

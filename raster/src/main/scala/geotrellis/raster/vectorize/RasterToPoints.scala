package geotrellis.raster.vectorize

import geotrellis.raster._
import geotrellis.vector.{Point, PointFeature, _}

import scala.collection.mutable.ArrayBuffer

object RasterToPoints {

  def fromInt(tile: Tile, extent: Extent) = {
    val cols = tile.cols
    val rows = tile.rows

    val points = ArrayBuffer.empty[PointFeature[Int]]
    val rasterExtent = RasterExtent(extent, cols, rows)

    for (col <- 0 until cols; row <- 0 until rows) {
      val value = tile.get(col, row)

      if (!isNoData(value)) {
        val (x, y) = rasterExtent.gridToMap(col, row)

        points += PointFeature(Point(x, y), value)
      }
    }

    points
  }

  def fromDouble(tile: Tile, extent: Extent) = {
    val cols = tile.cols
    val rows = tile.rows

    val points = ArrayBuffer.empty[PointFeature[Double]]
    val rasterExtent = RasterExtent(extent, cols, rows)

    for (col <- 0 until cols; row <- 0 until rows) {
      val value = tile.getDouble(col, row)

      if (!isNoData(value)) {
        val (x, y) = rasterExtent.gridToMap(col, row)

        points += PointFeature(Point(x, y), value)
      }
    }

    points
  }
}

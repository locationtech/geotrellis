package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.tiling._
import geotrellis.vector.Extent

import geotrellis.proj4.CRS

import org.apache.spark.rdd._

case class RasterMetaData(
  cellType: CellType,
  extent: Extent,
  crs: CRS,
  tileLayout: TileLayout
) {
  lazy val mapTransform = MapKeyTransform(crs, tileLayout.tileDimensions)

  def tileTransform(tileScheme: TileScheme): TileKeyTransform = tileScheme(tileLayout.tileCols, tileLayout.tileRows)
}

object RasterMetaData {
  def fromRdd[T](rdd: RDD[(T, Tile)], crs: CRS, tileLayout: TileLayout, isUniform: Boolean = true)(getExtent: T => Extent): RasterMetaData = {
    val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) =
      if(isUniform) {
        val (key, tile) = rdd.first
        val extent = getExtent(key)
        (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows))
      } else {
        rdd
          .map { case (key, tile) =>
            val extent = getExtent(key)
            (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows))
        }
          .reduce { (t1, t2) =>
          val (e1, ct1, cs1) = t1
          val (e2, ct2, cs2) = t2
          (
            e1.combine(e2),
            ct1.union(ct2),
            if(cs1.resolution < cs2.resolution) cs1 else cs2
          )
        }
      }

    val worldExtent = crs.worldExtent

    val extentIntersection = worldExtent.intersection(uncappedExtent).get

    RasterMetaData(cellType, extentIntersection, crs, tileLayout)
  }
}

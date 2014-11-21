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

  lazy val gridBounds: GridBounds = mapTransform(extent)

  def tileTransform(tileScheme: TileScheme): TileKeyTransform = tileScheme(tileLayout.layoutCols, tileLayout.layoutRows)
}

object RasterMetaData {
  def envelopeExtent[T](rdd: RDD[(T, Tile)])(getExtent: T => Extent): (Extent, CellType, CellSize) = {
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
          if (cs1.resolution < cs2.resolution) cs1 else cs2
        )
      }
  }

  /**
   * Compose Extents from given raster tiles and fit it on given [[TileLayout]]
   */
  def fromRdd[T](rdd: RDD[(T, Tile)], crs: CRS, tileLayout: TileLayout)
                (getExtent: T => Extent): RasterMetaData = {
    val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) = envelopeExtent(rdd)(getExtent)
    val worldExtent = crs.worldExtent
    val extentIntersection = worldExtent.intersection(uncappedExtent).get
    RasterMetaData(cellType, extentIntersection, crs, tileLayout)
  }

  /**
   * Compose Extents from given raster tiles and pick the closest [[LayoutLevel]] in the [[LayoutScheme]].
   * @param isUniform   all the tiles in the RDD are known to have the same extent
   */
  def fromRdd[T](rdd: RDD[(T, Tile)], crs: CRS, layoutScheme: LayoutScheme, isUniform: Boolean = false)
                (getExtent: T => Extent): (LayoutLevel, RasterMetaData) = {
    val (uncappedExtent, cellType, cellSize): (Extent, CellType, CellSize) =
      if(isUniform) {
        val (key, tile) = rdd.first
        val extent = getExtent(key)
        (extent, tile.cellType, CellSize(extent, tile.cols, tile.rows))
      } else {
        envelopeExtent(rdd)(getExtent)
      }

    val worldExtent = crs.worldExtent
    val layoutLevel: LayoutLevel = layoutScheme.levelFor(worldExtent, cellSize)
    val extentIntersection = worldExtent.intersection(uncappedExtent).get
    layoutLevel -> RasterMetaData(cellType, extentIntersection, crs, layoutLevel.tileLayout)
  }
}

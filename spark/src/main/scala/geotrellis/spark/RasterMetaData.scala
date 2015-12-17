package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.tiling._
import geotrellis.vector.{ProjectedExtent, Extent}

import geotrellis.proj4.CRS

import org.apache.spark.rdd._

/**
 * @param cellType    value type of each cell
 * @param layout      definition of the tiled raster layout
 * @param extent      Extent covering the source data
 * @param crs         CRS of the raster projection
 */
case class RasterMetaData(
  cellType: CellType,
  layout: LayoutDefinition,
  extent: Extent,
  crs: CRS
) {
  /** Transformations between tiling scheme and map references */
  def mapTransform = layout.mapTransform
  /** Layout raster extent */
  def rasterExtent = layout.rasterExtent
  /** TileLayout of the layout */
  def tileLayout = layout.tileLayout
  /** Full extent of the layout */
  def layoutExtent = layout.extent
  /** GridBounds of data tiles in the layout */
  def gridBounds = mapTransform(extent)

  def tileTransform(tileScheme: TileScheme): TileKeyTransform =
    tileScheme(layout.tileLayout.layoutCols, layout.tileLayout.layoutRows)

  def combine(other: RasterMetaData) = {
    val combinedExtent       = extent combine other.extent
    val combinedLayoutExtent = layout.extent combine other.layout.extent
    val combinedTileLayout   = layout.tileLayout combine other.layout.tileLayout

    this
      .copy(
        extent = combinedExtent,
        layout = this.layout
          .copy(
            extent     = combinedLayoutExtent,
            tileLayout = combinedTileLayout
          )
      )
  }
}

object RasterMetaData {
  def envelopeExtent[K, V <: CellGrid](rdd: RDD[(K, V)])(getExtent: K => Extent): (Extent, CellType, CellSize) = {
    rdd
      .map { case (key, grid) =>
        val extent = getExtent(key)
        (extent, grid.cellType, CellSize(extent, grid.cols, grid.rows))
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
  def fromRdd[K, V <: CellGrid](rdd: RDD[(K, V)], crs: CRS, layout: LayoutDefinition)
                (getExtent: K => Extent): RasterMetaData = {
    val (uncappedExtent, cellType, _) = envelopeExtent(rdd)(getExtent)
    RasterMetaData(cellType, layout, uncappedExtent, crs)
  }

  /**
   * Compose Extents from given raster tiles and use [[LayoutScheme]] to create the [[LayoutDefinition]].
   */
  def fromRdd[K, V <: CellGrid](rdd: RDD[(K, V)], crs: CRS, scheme: LayoutScheme)
                (getExtent: K => Extent): (Int, RasterMetaData) = {
    val (uncappedExtent, cellType, cellSize) = envelopeExtent(rdd)(getExtent)
    val LayoutLevel(zoom, layout) = scheme.levelFor(uncappedExtent, cellSize)
    (zoom, RasterMetaData(cellType, layout, uncappedExtent, crs))
  }
}

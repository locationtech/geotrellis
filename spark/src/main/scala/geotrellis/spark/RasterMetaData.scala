package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.tiling._
import geotrellis.spark.ingest._
import geotrellis.vector.{ProjectedExtent, Extent}

import geotrellis.proj4.CRS

import org.apache.spark.rdd._

/**
 * @param cellType    value type of each cell
 * @param layout      definition of the tiled raster layout
 * @param extent      Extent covering the source data
 * @param crs         CRS of the raster projection
 */
case class RasterMetaData[K](
  cellType: CellType,
  layout: LayoutDefinition,
  extent: Extent,
  crs: CRS,
  keyBounds: KeyBounds[K]
) {
  /** Transformations between tiling scheme and map references */
  def mapTransform = layout.mapTransform
  /** TileLayout of the layout */
  def tileLayout = layout.tileLayout
  /** Full extent of the layout */
  def layoutExtent = layout.extent
  /** GridBounds of data tiles in the layout */
  def gridBounds = mapTransform(extent)

  def tileTransform(tileScheme: TileScheme): TileKeyTransform =
    tileScheme(layout.tileLayout.layoutCols, layout.tileLayout.layoutRows)

  def combine(other: RasterMetaData[K]): RasterMetaData[K] = {
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
  implicit def toLayoutDefinition(md: RasterMetaData[_]): LayoutDefinition =
    md.layout

  implicit def toMapKeyTransform(md: RasterMetaData[_]): MapKeyTransform =
    md.layout.mapTransform

  implicit def toSpatialKeyBounds(md: RasterMetaData[SpatialKey]): KeyBounds[SpatialKey] =
    KeyBounds(SpatialKey(md.gridBounds.colMin, md.gridBounds.rowMin),
              SpatialKey(md.gridBounds.colMax, md.gridBounds.rowMax))

  def collectMetadata[
    K: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)]): (Extent, CellType, CellSize, KeyBounds[K2]) = {
    rdd
      .map { case (key, grid) =>
        val extent = key.extent
        val boundsKey = key.translate(SpatialKey(0,0))
        (extent, grid.cellType, CellSize(extent, grid.cols, grid.rows), KeyBounds(boundsKey, boundsKey))
      }
      .reduce { (tuple1, tuple2) =>
        val (extent1, cellType1, cellSize1, bounds1) = tuple1
        val (extent2, cellType2, cellSize2, bounds2) = tuple2
        (
          extent1.combine(extent2),
          cellType1.union(cellType2),
          if (cellSize1.resolution < cellSize2.resolution) cellSize1 else cellSize2,
          bounds1.combine(bounds2)
        )
      }
  }

  /**
    * Compose Extents from given raster tiles and fit it on given [[TileLayout]]
    */
  def fromRdd[K: (? => TilerKeyMethods[K, K2]) , V <: CellGrid, K2: SpatialComponent: Boundable](rdd: RDD[(K, V)], crs: CRS, layout: LayoutDefinition): RasterMetaData[K2] = {
    val (extent: Extent, cellType, _, bounds) = collectMetadata(rdd)
    val GridBounds(colMin, rowMin, colMax, rowMax) = layout.mapTransform(extent)
    val kb: KeyBounds[K2] =
      KeyBounds(bounds.minKey.updateSpatialComponent(SpatialKey(colMin, rowMin)),
                bounds.maxKey.updateSpatialComponent(SpatialKey(colMax, rowMax)))
    RasterMetaData(cellType, layout, extent, crs, kb)
  }

  /**
   * Compose Extents from given raster tiles and use [[LayoutScheme]] to create the [[LayoutDefinition]].
   */
  def fromRdd[K: (? => TilerKeyMethods[K, K2]) , V <: CellGrid, K2: SpatialComponent: Boundable](rdd: RDD[(K, V)], crs: CRS, scheme: LayoutScheme): (Int, RasterMetaData[K2]) = {
    val (extent: Extent, cellType, cellSize, bounds) = collectMetadata(rdd)
    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val GridBounds(colMin, rowMin, colMax, rowMax) = layout.mapTransform(extent)
    val kb: KeyBounds[K2] =
      KeyBounds(bounds.minKey.updateSpatialComponent(SpatialKey(colMin, rowMin)),
                bounds.maxKey.updateSpatialComponent(SpatialKey(colMax, rowMax)))
    (zoom, RasterMetaData(cellType, layout, extent, crs, kb))
  }

  def fromRdd[K: ProjectedExtentComponent: (? => TilerKeyMethods[K, K2]) , V <: CellGrid, K2: SpatialComponent: Boundable](rdd: RDD[(K, V)], scheme: LayoutScheme): (Int, RasterMetaData[K2]) = {
    val (extent: Extent, cellType, cellSize, crsSet, bounds) =
      rdd
        .map { case (key, grid) =>
          val ProjectedExtent(extent, crs) = key.projectedExtent
          val boundsKey = key.translate(SpatialKey(0,0))
          (extent, grid.cellType, CellSize(extent, grid.cols, grid.rows), Set(crs), KeyBounds(boundsKey, boundsKey))
        }
        .reduce { (tuple1, tuple2) =>
          val (extent1, cellType1, cellSize1, crs1, bounds1) = tuple1
          val (extent2, cellType2, cellSize2, crs2, bounds2) = tuple2
          (
            extent1.combine(extent2),
            cellType1.union(cellType2),
            if (cellSize1.resolution < cellSize2.resolution) cellSize1 else cellSize2,
            crs1 ++ crs2,
            bounds1.combine(bounds2)
            )
        }

    require(crsSet.size == 1, s"Multiple CRS tags found in source tiles: $crsSet")
    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val GridBounds(colMin, rowMin, colMax, rowMax) = layout.mapTransform(extent)
    val kb: KeyBounds[K2] =
      KeyBounds(bounds.minKey.updateSpatialComponent(SpatialKey(colMin, rowMin)),
                bounds.maxKey.updateSpatialComponent(SpatialKey(colMax, rowMax)))
    (zoom, RasterMetaData(cellType, layout, extent, crsSet.head, kb))
  }
}
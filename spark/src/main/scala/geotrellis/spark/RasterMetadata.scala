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
case class RasterMetadata[K](
  cellType: CellType,
  layout: LayoutDefinition,
  extent: Extent,
  crs: CRS,
  bounds: Bounds[K]
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

  def combine(other: RasterMetadata[K]): RasterMetadata[K] = {
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

object RasterMetadata {
  implicit def toLayoutDefinition(md: RasterMetadata[_]): LayoutDefinition =
    md.layout

  implicit def toMapKeyTransform(md: RasterMetadata[_]): MapKeyTransform =
    md.layout.mapTransform

  implicit def boundsComponent[K]: Component[RasterMetadata[K], Bounds[K]] =
    Component(_.bounds, (md, b) => md.copy(bounds = b))

  implicit def mergable[K]: merge.Mergable[RasterMetadata[K]] =
    new merge.Mergable[RasterMetadata[K]] {
      def merge(t1: RasterMetadata[K], t2: RasterMetadata[K]): RasterMetadata[K] =
        t1.combine(t2)
    }

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

  def collectMetadataWithCRS[
    K: Component[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)]): (Extent, CellType, CellSize, KeyBounds[K2], CRS) = {
    val (extent, cellType, cellSize, crsSet, bounds) =
      rdd
      .map { case (key, grid) =>
        val ProjectedExtent(extent, crs) = key.getComponent[ProjectedExtent]
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
    require(crsSet.size == 1, s"Multiple CRS tags found: $crsSet")
    (extent, cellType, cellSize, bounds, crsSet.head)
  }

  /**
    * Compose Extents from given raster tiles and fit it on given [[TileLayout]]
    */
  def fromRdd[
    K: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, layout: LayoutDefinition): RasterMetadata[K2] = {
    val (extent, cellType, _, bounds) = collectMetadata(rdd)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    RasterMetadata(cellType, layout, extent, crs, kb)
  }

  /**
   * Compose Extents from given raster tiles and use [[LayoutScheme]] to create the [[LayoutDefinition]].
   */
  def fromRdd[
    K: (? => TilerKeyMethods[K, K2]) ,
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], crs: CRS, scheme: LayoutScheme): (Int, RasterMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds) = collectMetadata(rdd)
    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, RasterMetadata(cellType, layout, extent, crs, kb))
  }

  def fromRdd[
    K: Component[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], scheme: LayoutScheme): (Int, RasterMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds, crs) = collectMetadataWithCRS(rdd)

    val LayoutLevel(zoom, layout) = scheme.levelFor(extent, cellSize)
    val GridBounds(colMin, rowMin, colMax, rowMax) = layout.mapTransform(extent)
    val kb = bounds.setSpatialBounds(KeyBounds(layout.mapTransform(extent)))
    (zoom, RasterMetadata(cellType, layout, extent, crs, kb))
  }

  def fromRdd[
    K: Component[?, ProjectedExtent]: (? => TilerKeyMethods[K, K2]),
    V <: CellGrid,
    K2: SpatialComponent: Boundable
  ](rdd: RDD[(K, V)], layoutDefinition: LayoutDefinition): (Int, RasterMetadata[K2]) = {
    val (extent, cellType, cellSize, bounds, crs) = collectMetadataWithCRS(rdd)
    val kb = bounds.setSpatialBounds(KeyBounds(layoutDefinition.mapTransform(extent)))
    (0, RasterMetadata(cellType, layoutDefinition, extent, crs, kb))
  }
}

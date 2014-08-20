package geotrellis.spark.rdd

import geotrellis.spark._
import geotrellis.raster._
import geotrellis.vector.Extent
import geotrellis.spark.tiling._
import geotrellis.proj4._

case class LayerMetaData(cellType: CellType, extent: Extent, crs: CRS, level: LayoutLevel, tileIndexScheme: TileIndexScheme) 
    extends MapGridTransformDelegate with IndexGridTransformDelegate with MapIndexTransform {
  lazy val mapGridTransform = MapGridTransform(crs, tileLayout.tileDimensions)
  lazy val indexGridTransform = tileIndexScheme(tileLayout.tileDimensions)

  lazy val gridBounds = mapToGrid(extent)
  lazy val tileIds = gridToIndex(gridBounds)
  def tileLayout = level.tileLayout

  def withCoordScheme(coordScheme: TileCoordScheme) = 
    TileCoordIndexMapTransform(coordScheme(tileLayout.tileCols, tileLayout.tileRows), indexGridTransform, mapGridTransform)
}

case class TileCoordIndexMapTransform(tileGridTransform: TileGridTransform, indexGridTransform: IndexGridTransform, mapGridTransform: MapGridTransform) 
    extends MapGridTransformDelegate with IndexGridTransformDelegate with MapIndexTransform with TileIndexTransform with TileMapTransform 

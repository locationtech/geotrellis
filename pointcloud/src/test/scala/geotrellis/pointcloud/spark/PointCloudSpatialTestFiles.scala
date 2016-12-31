package geotrellis.pointcloud.spark

import geotrellis.pointcloud.spark._
import geotrellis.pointcloud.spark.io._
import geotrellis.pointcloud.spark.io.hadoop._
import geotrellis.proj4.CRS
import geotrellis.raster.{DoubleConstantNoDataCellType, TileLayout}
import geotrellis.spark.{ContextRDD, KeyBounds, SpatialKey, TileLayerMetadata}
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent

trait PointCloudSpatialTestFiles { self: PointCloudTestEnvironment =>
  val extent = Extent(635609.85, 848889.7, 638992.55, 853545.43)
  val crs = CRS.fromEpsgCode(20255)
  val rdd = HadoopPointCloudRDD(lasPath).flatMap(_._2)
  val layoutDefinition = LayoutDefinition(
    extent,
    TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
  val tiledWithLayout = rdd.tileToLayout(layoutDefinition)
  val gb = layoutDefinition.mapTransform(extent)

  val md =
    TileLayerMetadata[SpatialKey](
      cellType = DoubleConstantNoDataCellType,
      layout = layoutDefinition,
      extent = extent,
      crs = crs,
      bounds = KeyBounds(gb)
    )

  val pointCloudSample = ContextRDD(tiledWithLayout, md)
}

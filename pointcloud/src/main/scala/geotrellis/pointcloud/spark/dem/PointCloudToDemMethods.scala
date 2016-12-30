package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

abstract class PointCloudToDemMethods[M: GetComponent[?, LayoutDefinition]](
  val self: RDD[(SpatialKey, PointCloud)] with Metadata[M]
) extends MethodExtensions[RDD[(SpatialKey, PointCloud)] with Metadata[M]] {
  def pointToGrid(cellSize: CellSize, options: PointToGrid.Options): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] =
    PointCloudToDem(self, cellSize, options)
}

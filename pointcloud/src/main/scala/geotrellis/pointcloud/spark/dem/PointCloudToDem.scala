package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

object PointCloudToDem {
  def apply[M: GetComponent[?, LayoutDefinition]](rdd: RDD[(SpatialKey, PointCloud)] with Metadata[M], cellSize: CellSize, options: PointToGrid.Options): RDD[(SpatialKey, Tile)] with Metadata[LayoutDefinition] = {
    val layoutDefinition = rdd.metadata.getComponent[LayoutDefinition]
    val mapTransform = layoutDefinition.mapTransform

    val result =
      rdd
        .collectNeighbors
        .mapPartitions({ partition =>
          partition.map { case (key, neighbors) =>
            val extent = mapTransform(key)
            val raster =
              PointToGrid.createRaster(neighbors.map(_._2._2), RasterExtent(extent, cellSize), options)
            (key, raster.tile)
          }
        }, preservesPartitioning = true)

    ContextRDD(result, layoutDefinition)
  }
}

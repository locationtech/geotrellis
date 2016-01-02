package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._

trait BaseRasterRDDMethods[K] extends RasterRDDMethods[K] with Serializable {
  def convert(cellType: CellType): RasterRDD[K] =
    rasterRDD.mapValues(_.convert(cellType), rasterRDD.metadata.copy(cellType = cellType))

  def asRasters()(implicit sc: SpatialComponent[K]): RDD[(K, Raster)] =
    rasterRDD.mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, rasterRDD.metadata.mapTransform(key)))
      }
    }, true)
}

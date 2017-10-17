package geotrellis.spark.io.file.geotiff

import geotrellis.raster.{CellGrid, Raster}
import geotrellis.spark.LayerId
import geotrellis.vector.Extent

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import java.net.URI

trait GeoTiffRDDLayerReader[T <: CellGrid] {
  val rdd: RDD[(Extent, URI)]
  val discriminator: URI => String

  def read(layerId: LayerId)(x: Int, y: Int)(implicit sc: SparkContext): Raster[T]
  def readAll(layerId: LayerId)(implicit sc: SparkContext): RDD[Raster[T]]
}

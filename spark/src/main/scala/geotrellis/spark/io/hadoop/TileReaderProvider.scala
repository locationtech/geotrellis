package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReaderProvider[K] {
  def reader(layerMetaData: HadoopLayerMetaData): Reader[K, Tile]
}

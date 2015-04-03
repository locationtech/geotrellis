package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReaderProvider[K] {
  def reader(instance: AccumuloInstance, layerId: LayerId, accumuloLayerMetaData: AccumuloLayerMetaData): Reader[K, Tile]
}

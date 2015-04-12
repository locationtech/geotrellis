package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReaderProvider[K] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[K]): KeyIndex[K]
  def reader(instance: AccumuloInstance, layerId: LayerId, accumuloLayerMetaData: AccumuloLayerMetaData, keyIndex: KeyIndex[K]): Reader[K, Tile]
}

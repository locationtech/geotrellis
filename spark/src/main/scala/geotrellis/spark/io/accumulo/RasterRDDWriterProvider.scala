package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait RasterRDDWriterProvider[K] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[K]): KeyIndex[K]
  def writer(instance: AccumuloInstance, layerMetaData: AccumuloLayerMetaData, keyBounds: KeyBounds[K], keyIndex: KeyIndex[K])(implicit sc: SparkContext): RasterRDDWriter[K]
}

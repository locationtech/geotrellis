package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait RasterRDDReaderProvider[K] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[K]): KeyIndex[K]
  def reader(instance: AccumuloInstance, metaData: AccumuloLayerMetaData, keyBounds: KeyBounds[K], index: KeyIndex[K])(implicit sc: SparkContext): FilterableRasterRDDReader[K]
}

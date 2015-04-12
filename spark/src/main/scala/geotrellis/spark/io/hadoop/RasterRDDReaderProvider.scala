package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait RasterRDDReaderProvider[K] {
  def index(tileLayout: TileLayout, keyBounds: KeyBounds[K]): KeyIndex[K]

  def reader(catalogConfig: HadoopRasterCatalogConfig, metaData: HadoopLayerMetaData, index: KeyIndex[K])(implicit sc: SparkContext): FilterableRasterRDDReader[K]
}

package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReaderProvider[K] {
  def reader(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[K]
  )(implicit sc: SparkContext): Reader[K, Tile]
}

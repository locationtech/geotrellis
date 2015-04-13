package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path

trait RasterRDDWriterProvider[K] {
  def writer(catalogConfig: HadoopRasterCatalogConfig, layerMetaData: HadoopLayerMetaData, index: KeyIndex[K], clobber: Boolean = true)(implicit sc: SparkContext): RasterRDDWriter[K]
}

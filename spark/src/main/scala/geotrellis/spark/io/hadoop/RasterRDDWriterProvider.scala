package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path

trait RasterRDDWriterProvider[K] {
  def writer(catalogConfig: RasterCatalogConfig, layerPath: Path, clobber: Boolean = true)(implicit sc: SparkContext): RasterRDDWriter[K]
}

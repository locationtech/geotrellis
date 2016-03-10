package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class MultibandGridKeyHadoopOutput extends HadoopOutput[GridKey, MultibandTile, RasterMetadata[GridKey]] {
  def writer(method: KeyIndexMethod[GridKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[GridKey, MultibandTile, RasterMetadata[GridKey]](method)
}

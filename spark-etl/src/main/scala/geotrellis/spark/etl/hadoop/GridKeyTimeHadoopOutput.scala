package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class GridTimeKeyHadoopOutput extends HadoopOutput[GridTimeKey, Tile, RasterMetadata[GridTimeKey]] {
  def writer(method: KeyIndexMethod[GridTimeKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[GridTimeKey, Tile, RasterMetadata[GridTimeKey]](method)
}

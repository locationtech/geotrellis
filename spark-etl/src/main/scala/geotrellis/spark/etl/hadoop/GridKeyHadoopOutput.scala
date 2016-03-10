package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class GridKeyHadoopOutput extends HadoopOutput[GridKey, Tile, RasterMetadata[GridKey]] {
  def writer(method: KeyIndexMethod[GridKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[GridKey, Tile, RasterMetadata[GridKey]](method)
}

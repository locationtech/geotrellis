package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class SpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey, Tile, RasterMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[SpaceTimeKey, Tile, RasterMetadata[SpaceTimeKey]](method)
}

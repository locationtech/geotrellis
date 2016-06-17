package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class SpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](method)
}

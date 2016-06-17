package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import org.apache.spark.SparkContext

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(credentials), props("table"), strategy(props)).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](method)
}

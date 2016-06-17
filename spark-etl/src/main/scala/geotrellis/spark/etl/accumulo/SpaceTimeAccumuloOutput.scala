package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(credentials), props("table"), strategy(props)).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](method)
}

package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.backend.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpatialAccumuloOutput extends AccumuloOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters, credentials: Option[Backend])(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(credentials), props("table"), strategy(props)).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](method)
}

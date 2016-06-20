package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class SpatialAccumuloOutput extends AccumuloOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(job.outputCredentials), job.outputProps("table"), strategy(job.outputProps)).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](job.config.ingestOptions.getKeyIndexMethod[SpatialKey])
}

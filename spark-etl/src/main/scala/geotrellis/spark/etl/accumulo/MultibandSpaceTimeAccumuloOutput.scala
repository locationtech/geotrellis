package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import org.apache.spark.SparkContext

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], job: EtlJob)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(job.outputCredentials), job.outputProps("table"), strategy(job.outputProps)).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](method)
}

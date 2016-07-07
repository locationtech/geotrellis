package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter

import org.apache.spark.SparkContext

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(job.outputCredentials), job.outputProps("table"), strategy(job.outputProps)).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](job.input.ingestOptions.getKeyIndexMethod[SpaceTimeKey])
}

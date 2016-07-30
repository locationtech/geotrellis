package geotrellis.spark.etl.hbase

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.HBaseLayerWriter

import org.apache.spark.SparkContext

class MultibandSpaceTimeHBaseOutput extends HBaseOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    HBaseLayerWriter(getInstance(job.conf.outputProfile), job.outputProps("table")).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](job.conf.output.getKeyIndexMethod[SpaceTimeKey])
}

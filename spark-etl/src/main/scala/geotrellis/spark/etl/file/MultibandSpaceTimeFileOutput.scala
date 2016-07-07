package geotrellis.spark.etl.file

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import org.apache.spark.SparkContext

class MultibandSpaceTimeFileOutput extends FileOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    FileLayerWriter(job.outputProps("path")).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](job.input.ingestOptions.getKeyIndexMethod[SpaceTimeKey])
}

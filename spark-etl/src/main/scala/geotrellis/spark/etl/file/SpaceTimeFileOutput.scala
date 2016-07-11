package geotrellis.spark.etl.file

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import org.apache.spark.SparkContext

class SpaceTimeFileOutput extends FileOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    FileLayerWriter(job.outputProps("path")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](job.conf.output.getKeyIndexMethod[SpaceTimeKey])
}

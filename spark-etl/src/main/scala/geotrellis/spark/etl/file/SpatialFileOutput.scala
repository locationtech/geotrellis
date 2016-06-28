package geotrellis.spark.etl.file

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.file._
import org.apache.spark.SparkContext

class SpatialFileOutput extends FileOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    FileLayerWriter(job.outputProps("path")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](job.config.ingestOptions.getKeyIndexMethod[SpatialKey])
}

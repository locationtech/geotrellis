package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.etl.config.Backend
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class SpatialHadoopOutput extends HadoopOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    HadoopLayerWriter(job.outputProps("path")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](job.conf.output.getKeyIndexMethod[SpatialKey])
}

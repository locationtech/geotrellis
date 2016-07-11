package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod
import org.apache.spark.SparkContext

class MultibandSpatialHadoopOutput extends HadoopOutput[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    HadoopLayerWriter(job.outputProps("path")).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](job.conf.output.getKeyIndexMethod[SpatialKey])
}

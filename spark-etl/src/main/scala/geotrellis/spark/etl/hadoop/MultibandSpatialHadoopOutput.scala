package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultibandTile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._

import org.apache.spark.SparkContext

class MultibandSpatialHadoopOutput extends HadoopOutput[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    HadoopLayerWriter(getPath(conf.output.backend).path).writer[SpatialKey, MultibandTile, TileLayerMetadata[SpatialKey]](conf.output.getKeyIndexMethod[SpatialKey])
}

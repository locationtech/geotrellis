package geotrellis.spark.etl.hadoop

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._

import org.apache.spark.SparkContext

class MultibandSpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    HadoopLayerWriter(getPath(conf.output.backend).path).writer[SpaceTimeKey, MultibandTile, TileLayerMetadata[SpaceTimeKey]](conf.output.getKeyIndexMethod[SpaceTimeKey])
}

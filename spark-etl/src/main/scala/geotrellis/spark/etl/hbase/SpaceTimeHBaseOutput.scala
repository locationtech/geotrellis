package geotrellis.spark.etl.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.HBaseLayerWriter

import org.apache.spark.SparkContext

class SpaceTimeHBaseOutput extends HBaseOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    HBaseLayerWriter(getInstance(conf.outputProfile), getPath(conf.output.backend).table).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](conf.output.getKeyIndexMethod[SpaceTimeKey])
}

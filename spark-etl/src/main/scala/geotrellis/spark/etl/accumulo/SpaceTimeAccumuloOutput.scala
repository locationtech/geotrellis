package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.config.EtlConf
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter

import org.apache.spark.SparkContext

class SpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(conf: EtlConf)(implicit sc: SparkContext) =
    AccumuloLayerWriter(getInstance(conf.outputProfile), getPath(conf.output.backend).table, strategy(conf.outputProfile)).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](conf.output.getKeyIndexMethod[SpaceTimeKey])
}

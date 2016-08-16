package geotrellis.spark.etl.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.HBaseLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext

class SpaceTimeHBaseOutput extends HBaseOutput[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters)(implicit sc: SparkContext) =
    HBaseLayerWriter(getInstance(props), props("table")).writer[SpaceTimeKey, Tile, TileLayerMetadata[SpaceTimeKey]](method)
}

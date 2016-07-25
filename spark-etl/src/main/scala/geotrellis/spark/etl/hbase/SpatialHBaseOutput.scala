package geotrellis.spark.etl.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.HBaseLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.spark.SparkContext

class SpatialHBaseOutput extends HBaseOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    HBaseLayerWriter(getInstance(props), props("table")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](method)
}

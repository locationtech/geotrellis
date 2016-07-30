package geotrellis.spark.etl.hbase

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.etl.EtlJob
import geotrellis.spark.io._
import geotrellis.spark.io.hbase.HBaseLayerWriter

import org.apache.spark.SparkContext

class SpatialHBaseOutput extends HBaseOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(job: EtlJob)(implicit sc: SparkContext) =
    HBaseLayerWriter(getInstance(job.conf.outputProfile), job.outputProps("table")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](job.conf.output.getKeyIndexMethod[SpatialKey])
}

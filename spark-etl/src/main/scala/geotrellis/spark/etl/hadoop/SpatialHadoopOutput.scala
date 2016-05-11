package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class SpatialHadoopOutput extends HadoopOutput[SpatialKey, Tile, TileLayerMetadata[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[SpatialKey, Tile, TileLayerMetadata[SpatialKey]](method)
}

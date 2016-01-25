package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.Path

class SpatialHadoopOutput extends HadoopOutput[SpatialKey] {
  def writer(props: Parameters) =
    HadoopLayerWriter[SpatialKey, Tile, RasterMetadata](new Path(props("path")))
}

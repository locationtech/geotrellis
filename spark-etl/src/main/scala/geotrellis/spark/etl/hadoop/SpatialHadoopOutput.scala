package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.Path

class SpatialHadoopOutput extends HadoopOutput[SpatialKey] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    HadoopLayerWriter[SpatialKey, Tile, RasterMetaData, RasterRDD[SpatialKey]](new Path(props("path")), method)
}

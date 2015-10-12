package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpatialKey, RasterRDD}
import org.apache.hadoop.fs.Path

class SpatialHadoopOutput extends HadoopOutput[SpatialKey] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    HadoopLayerWriter[SpatialKey, Tile, RasterRDD](new Path(props("path")), method)
}
package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.{SpaceTimeKey, RasterRDD}
import org.apache.hadoop.fs.Path

class SpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    HadoopLayerWriter[SpaceTimeKey, Tile, RasterRDD](new Path(props("path")), method)
}
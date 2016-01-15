package geotrellis.spark.etl.hadoop

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.json._
import geotrellis.spark.SpaceTimeKey

import org.apache.hadoop.fs.Path

class SpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey] {
  def writer(props: Parameters) =
    HadoopLayerWriter[SpaceTimeKey, Tile, RasterMetaData](new Path(props("path")))
}

package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.json._

import org.apache.hadoop.fs.Path

class MultibandSpatialHadoopOutput extends HadoopOutput[SpatialKey, MultiBandTile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters) =
    HadoopLayerWriter[SpatialKey, MultiBandTile, RasterMetaData](new Path(props("path")), method)
}

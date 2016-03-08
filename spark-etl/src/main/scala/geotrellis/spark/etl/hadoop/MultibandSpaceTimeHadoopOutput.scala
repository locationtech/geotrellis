package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.SpaceTimeKey

import org.apache.hadoop.fs.Path

class MultibandSpaceTimeHadoopOutput extends HadoopOutput[SpaceTimeKey, MultiBandTile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    HadoopLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData](new Path(props("path")), method)
}


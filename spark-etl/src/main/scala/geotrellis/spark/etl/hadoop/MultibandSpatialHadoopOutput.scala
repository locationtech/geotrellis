package geotrellis.spark.etl.hadoop

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index.KeyIndexMethod

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

class MultibandSpatialHadoopOutput extends HadoopOutput[SpatialKey, MultiBandTile, RasterMetaData[SpatialKey]] {
  def writer(method: KeyIndexMethod[SpatialKey], props: Parameters)(implicit sc: SparkContext) =
    HadoopLayerWriter(props("path")).writer[SpatialKey, MultiBandTile, RasterMetaData[SpatialKey]](method)
}

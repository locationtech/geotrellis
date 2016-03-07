package geotrellis.spark.etl.accumulo

import geotrellis.raster.MultiBandTile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.accumulo.AccumuloLayerWriter

class MultibandSpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, MultiBandTile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    AccumuloLayerWriter[SpaceTimeKey, MultiBandTile, RasterMetaData](getInstance(props), props("table"), method)
}

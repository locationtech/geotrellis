package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod

class SpaceTimeAccumuloOutput extends AccumuloOutput[SpaceTimeKey, Tile, RasterMetaData] {
  def writer(method: KeyIndexMethod[SpaceTimeKey], props: Parameters) =
    AccumuloLayerWriter[SpaceTimeKey, Tile, RasterMetaData](getInstance(props),  props("table"), method)
}

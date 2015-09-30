package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import scala.reflect._

class SpaceTimeAccumuloOutput extends AccumuloOutput {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    AccumuloLayerWriter[SpaceTimeKey, Tile, RasterRDD](getInstance(props),  props("table"), method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]])
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}

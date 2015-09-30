package geotrellis.spark.etl.accumulo

import geotrellis.raster.Tile
import geotrellis.spark._
import geotrellis.spark.io.accumulo.AccumuloLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import scala.reflect._
import geotrellis.spark.io.avro.codecs._

class SpatialAccumuloOutput extends AccumuloOutput {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    AccumuloLayerWriter[SpatialKey, Tile, RasterRDD](getInstance(props),  props("table"), method.asInstanceOf[KeyIndexMethod[SpatialKey]])
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}
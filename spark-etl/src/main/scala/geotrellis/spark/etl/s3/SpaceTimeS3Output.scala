package geotrellis.spark.etl.s3

import geotrellis.raster.Tile
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3LayerWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.{SpaceTimeKey, LayerId, RasterRDD}
import scala.reflect._

class SpaceTimeS3Output extends S3Output {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    S3LayerWriter[SpaceTimeKey, Tile, RasterRDD](props("bucket"), props("key"), method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]])
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}

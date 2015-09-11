package geotrellis.spark.etl.s3

import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.RasterRDDWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.{SpaceTimeKey, LayerId, RasterRDD}
import scala.reflect._

class SpaceTimeS3Output extends S3Output {
  val key = classTag[SpaceTimeKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    new RasterRDDWriter[SpaceTimeKey](props("bucket"), props("key"), method.asInstanceOf[KeyIndexMethod[SpaceTimeKey]])(attributes(props))
      .write(id, rdd.asInstanceOf[RasterRDD[SpaceTimeKey]])
  }
}

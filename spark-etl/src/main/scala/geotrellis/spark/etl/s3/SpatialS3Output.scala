package geotrellis.spark.etl.s3

import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.RasterRDDWriter
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.{LayerId, RasterRDD, SpatialKey}
import scala.reflect._

class SpatialS3Output extends S3Output {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    new RasterRDDWriter[SpatialKey](props("bucket"), props("key"), method.asInstanceOf[KeyIndexMethod[SpatialKey]])(attributes(props))
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}


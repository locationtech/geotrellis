package geotrellis.spark.etl.s3

import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.spark.io.s3.S3RasterCatalog
import geotrellis.spark.{LayerId, RasterRDD, SpatialKey}
import scala.reflect._

class SpatialS3Sink extends S3Sink {
  val key = classTag[SpatialKey]

  def apply[K](id: LayerId, rdd: RasterRDD[K], method: KeyIndexMethod[K], props: Map[String, String]) = {
    S3RasterCatalog(props("bucket"), props("key"))(rdd.sparkContext)
      .writer[SpatialKey](method.asInstanceOf[KeyIndexMethod[SpatialKey]], props.getOrElse("subDir",""), props.getOrElse("clobber", "false").toBoolean)
      .write(id, rdd.asInstanceOf[RasterRDD[SpatialKey]])
  }
}


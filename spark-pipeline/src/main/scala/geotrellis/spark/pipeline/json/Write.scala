package geotrellis.spark.pipeline.json

import geotrellis.spark.{Bounds, LayerId, Metadata}
import geotrellis.spark.io.LayerWriter
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.file.FileLayerWriter
import geotrellis.spark.io.hadoop.HadoopLayerWriter
import geotrellis.spark.io.index.KeyIndexMethod
import geotrellis.util.GetComponent

import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

trait Write extends PipelineExpr {
  val name: String
  val profile: String
  val uri: String
  val pyramid: Boolean // true | false
  val maxZoom: Option[Int]
  val keyIndexMethod: PipelineKeyIndexMethod
  def writer: LayerWriter[LayerId]

  def eval[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](tuple: (Int, RDD[(K, V)] with Metadata[M])) = {
    val (zoom, rdd) = tuple
    // get it from the PipelineKeyIndexMethod
    writer.write(LayerId(name, zoom), rdd, null: KeyIndexMethod[K])
    tuple
  }
}
case class WriteFile(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = FileLayerWriter(uri)
}

case class WriteHadoop(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
)(implicit sc: SparkContext) extends Write {
  lazy val writer = HadoopLayerWriter(new Path(uri))
}

case class WriteS3(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteAccumulo(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteCassandra(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

case class WriteHBase(
  `type`: String,
  name: String,
  profile: String,
  uri: String,
  pyramid: Boolean,
  keyIndexMethod: PipelineKeyIndexMethod,
  maxZoom: Option[Int] = None
) extends Write {
  lazy val writer = ???
}

package geotrellis.spark.io.s3

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.json._
import geotrellis.spark.io.index._
import org.apache.spark._
import spray.json.JsonFormat

import scala.reflect._

class S3RasterCatalog(bucket: String, root: String)(implicit sc: SparkContext)
{
  val attributeStore = S3AttributeStore(bucket, root)
  def reader[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag] =
    new RasterRDDReader[K](attributeStore)

  def writer[K: SpatialComponent: Boundable: AvroRecordCodec: JsonFormat: ClassTag](keyIndexMethod: KeyIndexMethod[K], clobber: Boolean = true) =
    new RasterRDDWriter[K](bucket, root, keyIndexMethod, clobber)(attributeStore)
}
package geotrellis.spark.io.s3

import com.typesafe.scalalogging.slf4j._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroRecordCodec
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io.json._
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class S3LayerUpdater[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
    val attributeStore: AttributeStore[JsonFormat],
    rddWriter: S3RDDWriter[K, V],
    clobber: Boolean = true)
  (implicit val cons: ContainerConstructor[K, V, Container])
  extends LayerUpdater[LayerId, K, V, Container with RDD[(K, V)]] with LazyLogging {

  def getS3Client: () => S3Client = () => S3Client.default

  def update(id: LayerId, rdd: RDD[(K, V)]) = {
    try {
      require(!attributeStore.layerExists(id) || clobber, s"$id already exists")
      implicit val sc = rdd.sparkContext
      implicit val mdFormat = cons.metaDataFormat

      val (existingHeader, _, existingKeyBounds, existingKeyIndex, _) =
        attributeStore.readLayerAttributes[S3LayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Schema](id)

      val boundable = implicitly[Boundable[K]]
      val keyBounds = boundable.getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])

      if (!boundable.includes(keyBounds.minKey, existingKeyBounds) || !boundable.includes(keyBounds.maxKey, existingKeyBounds))
        throw new OutOfKeyBoundsError(id)

      val prefix = existingHeader.key
      val bucket = existingHeader.bucket

      val maxWidth = maxIndexWidth(existingKeyIndex.toIndex(existingKeyBounds.maxKey))
      val keyPath = (key: K) => makePath(prefix, encodeIndex(existingKeyIndex.toIndex(key), maxWidth))

      logger.info(s"Saving RDD ${rdd.name} to $bucket  $prefix")
      rddWriter.write(rdd, bucket, keyPath, oneToOne = false)
    } catch {
      case e: Exception => throw new LayerUpdateError(id).initCause(e)
    }
  }
}

object S3LayerUpdater {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
      bucket: String,
      prefix: String,
      clobber: Boolean = true)
    (implicit cons: ContainerConstructor[K, V, Container[K]]): S3LayerUpdater[K, V, Container[K]] =
    new S3LayerUpdater(
      S3AttributeStore(bucket, prefix),
      new S3RDDWriter[K, V],
      clobber
    )
}

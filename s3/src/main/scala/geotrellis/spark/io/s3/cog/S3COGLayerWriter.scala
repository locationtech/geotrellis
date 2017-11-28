package geotrellis.spark.io.s3.cog

import geotrellis.raster._
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.cog.COGLayer.ContextGeoTiff
import geotrellis.spark.io.index.{Index, KeyIndexMethod}
import geotrellis.spark.io.s3.{S3AttributeStore, makePath}
import geotrellis.spark.util._

import org.apache.spark.rdd._
import spray.json.JsonFormat
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import java.io.ByteArrayInputStream

import scala.reflect.{ClassTag, classTag}

class S3COGLayerWriter(
  val getAttributeStore: () => S3AttributeStore,
  bucket: String,
  keyPrefix: String
) extends Serializable {
  def write[
    K: SpatialComponent: JsonFormat: ClassTag,
    V <: CellGrid: ClassTag
  ](cogs: RDD[(K, ContextGeoTiff[K, V])])(id: LayerId, keyIndexMethod: KeyIndexMethod[K]) = {
    // schema for compatability purposes
    val schema = KryoWrapper(KeyValueRecordCodec[SpatialKey, Tile].schema)
    val conf = HadoopConfiguration(cogs.sparkContext.hadoopConfiguration)
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])

    // headers would be stored AS IS
    // right now we are duplicating data
    // metadata can be stored more effificent
    // as a single JSON for each partial pyramid
    cogs.foreachPartition { partition =>
      implicit val format: JsonFormat[K] = kwFomat.value
      implicit val metadataFormat: JsonFormat[TileLayerMetadata[K]] =
        tileLayerMetadataFormat[K](implicitly[SpatialComponent[K]], format)

      val attributeStore = getAttributeStore()
      val s3Client = attributeStore.s3Client

      partition.foreach { case (key, tiff) =>
        val zoomRanges =
          tiff.zoomRanges.getOrElse(throw new Exception(s"No zoomRanges for the key: $key"))

        // prefix to the real file destination, it's a partial pyramid
        val prefix = makePath(keyPrefix, s"${id.name}/${zoomRanges._1}_${zoomRanges._2}")

        // header of all layers wich correspons to the current cog
        val header = S3COGLayerHeader(
          keyClass = classTag[K].toString(),
          valueClass = classTag[V].toString(),
          bucket = bucket,
          key = prefix,
          zoomRanges = zoomRanges,
          layoutScheme = tiff.layoutScheme
        )

        // base layer attributes
        val metadata = tiff.metadata
        val keyIndex = keyIndexMethod.createIndex(metadata.bounds.asInstanceOf[KeyBounds[K]])
        attributeStore.writeLayerAttributes(id.copy(zoom = tiff.zoom), header, metadata, keyIndex, schema.value)

        // overviews attributes
        tiff.overviews.foreach { case (zoom, metadata) =>
          val keyIndex = keyIndexMethod.createIndex(metadata.bounds.asInstanceOf[KeyBounds[K]])
          attributeStore.writeLayerAttributes(id.copy(zoom = zoom), header, metadata, keyIndex, schema.value)
        }

        val bytes = GeoTiffWriter.write(tiff.geoTiff, true)
        val objectMetadata = new ObjectMetadata()
        objectMetadata.setContentLength(bytes.length)

        val is = new ByteArrayInputStream(bytes)
        val lastKeyIndex = keyIndexMethod.createIndex(tiff.overviews.last._2.bounds.asInstanceOf[KeyBounds[K]])
        val maxWidth = Index.digits(lastKeyIndex.toIndex(lastKeyIndex.keyBounds.maxKey))
        val keyPath = (key: K) => makePath(prefix, Index.encode(lastKeyIndex.toIndex(key), maxWidth))

        val p = new PutObjectRequest(bucket, s"${keyPath(key)}.tiff", is, objectMetadata)

        s3Client.putObject(p)
      }
    }
  }
}

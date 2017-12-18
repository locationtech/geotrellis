package geotrellis.spark.io.file.cog

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.cog.COGLayer.ContextGeoTiff
import geotrellis.spark.io.cog.vrt._
import geotrellis.spark.io.file._
import geotrellis.spark.io.index.{Index, KeyIndexMethod}
import geotrellis.spark.util._
import geotrellis.util.Filesystem

import org.apache.spark.rdd._
import spray.json.JsonFormat

import scala.collection.JavaConverters._
import scala.reflect.{ClassTag, classTag}
import scala.xml.Elem
import java.io.File


class FileCOGLayerWriter(
  val attributeStore: FileAttributeStore,
  catalogPath: String
) extends Serializable {

  def write[
    K: SpatialComponent: Boundable: JsonFormat : ClassTag,
    V <: CellGrid : ClassTag
  ](cogs: RDD[(K, ContextGeoTiff[K, V])])(id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    val catalogPathFile = new File(catalogPath)
    val path = LayerPath(id)

    val layerPath = new File(catalogPath, path).getAbsolutePath
    Filesystem.ensureDirectory(layerPath)

    // schema for compatability purposes
    val schema = KryoWrapper(KeyValueRecordCodec[SpatialKey, Tile].schema)
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])



    // headers would be stored AS IS
    // right now we are duplicating data
    // metadata can be stored more effificent
    // as a single JSON for each partial pyramid
    cogs.foreachPartition { partition: Iterator[(K, ContextGeoTiff[K, V])] =>
      implicit val format: JsonFormat[K] = kwFomat.value
      implicit val metadataFormat: JsonFormat[TileLayerMetadata[K]] =
        tileLayerMetadataFormat[K](implicitly[SpatialComponent[K]], format)

      partition foreach { case (key, tiff) =>
        val zoomRanges =
          tiff.zoomRanges.getOrElse(throw new Exception(s"No zoomRanges for the key: $key"))

        val header =
          FileCOGLayerHeader(
            keyClass = classTag[K].toString(),
            valueClass = classTag[V].toString(),
            path = path,
            zoomRanges = zoomRanges,
            layoutScheme = tiff.layoutScheme
          )

        // base layer attributes
        val metadata = tiff.metadata
        val keyBounds = metadata.bounds match {
          case kb: KeyBounds[K] => kb
          case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
        }
        val keyIndex = keyIndexMethod.createIndex(keyBounds)
        attributeStore.writeLayerAttributes(id.copy(zoom = tiff.zoom), header, metadata, keyIndex, schema.value)

        // overviews attributes
        tiff.overviews.foreach { case (zoom, ovrMetadata) =>
          val ovrKeyBounds = ovrMetadata.bounds match {
            case kb: KeyBounds[K] => kb
            case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
          }

          val keyIndex = keyIndexMethod.createIndex(ovrKeyBounds)
          attributeStore.writeLayerAttributes(id.copy(zoom = zoom), header, ovrMetadata, keyIndex, schema.value)
        }

        val lastKeyIndex =
          if (tiff.overviews.nonEmpty) {
            val lastKeyBounds = tiff.overviews.last._2.bounds match {
              case kb: KeyBounds[K] => kb
              case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
            }
            keyIndexMethod.createIndex(lastKeyBounds)
          }
          else keyIndexMethod.createIndex(keyBounds)

        val maxWidth = Index.digits(lastKeyIndex.toIndex(lastKeyIndex.keyBounds.maxKey))
        val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)

        tiff.geoTiff.write(s"${keyPath(key)}.tiff", true)
      }
    }
  }

  def writeVRT[
    K: SpatialComponent : Boundable : JsonFormat : ClassTag,
    V <: CellGrid : ClassTag
  ](cogs: RDD[(K, ContextGeoTiff[K, V])])(id: LayerId, keyIndexMethod: KeyIndexMethod[K], vrtOnly: Boolean = false): VRT[K] = {
    val sc = cogs.sparkContext
    val vrts = sc.collectionAccumulator[VRT[K]](s"vrt_$id")
    val samplesAccumulator = sc.collectionAccumulator[(Int, Elem)](s"vrt_samples_$id")

    val catalogPathFile = new File(catalogPath)
    val path = LayerPath(id)

    val layerPath = new File(catalogPath, path).getAbsolutePath
    Filesystem.ensureDirectory(layerPath)

    // schema for compatability purposes
    val schema = KryoWrapper(KeyValueRecordCodec[SpatialKey, Tile].schema)
    val kwFomat = KryoWrapper(implicitly[JsonFormat[K]])

    // headers would be stored AS IS
    // right now we are duplicating data
    // metadata can be stored more effificent
    // as a single JSON for each partial pyramid
    cogs.foreachPartition { partition: Iterator[(K, ContextGeoTiff[K, V])] =>
      implicit val format: JsonFormat[K] = kwFomat.value
      implicit val metadataFormat: JsonFormat[TileLayerMetadata[K]] =
        tileLayerMetadataFormat[K](implicitly[SpatialComponent[K]], format)

      partition foreach { case (key, tiff) =>
        val zoomRanges =
          tiff.zoomRanges.getOrElse(throw new Exception(s"No zoomRanges for the key: $key"))

        val header =
          FileCOGLayerHeader(
            keyClass = classTag[K].toString(),
            valueClass = classTag[V].toString(),
            path = path,
            zoomRanges = zoomRanges,
            layoutScheme = tiff.layoutScheme
          )

        val bandsCount = {
          if (classTag[V].runtimeClass.isAssignableFrom(classTag[MultibandTile].runtimeClass))
            tiff.geoTiff.tile.asInstanceOf[MultibandTile].bandCount
          else 1
        }

        // base layer attributes
        val metadata = tiff.metadata

        // create vrt
        val vrt = VRT(metadata)
        vrts.add(vrt)

        val keyBounds = metadata.bounds match {
          case kb: KeyBounds[K] => kb
          case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
        }
        val keyIndex = keyIndexMethod.createIndex(keyBounds)
        attributeStore.writeLayerAttributes(id.copy(zoom = tiff.zoom), header, metadata, keyIndex, schema.value)

        // overviews attributes
        tiff.overviews.foreach { case (zoom, ovrMetadata) =>
          val ovrKeyBounds = ovrMetadata.bounds match {
            case kb: KeyBounds[K] => kb
            case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
          }

          val keyIndex = keyIndexMethod.createIndex(ovrKeyBounds)
          attributeStore.writeLayerAttributes(id.copy(zoom = zoom), header, ovrMetadata, keyIndex, schema.value)
        }

        val geoTiff = tiff.geoTiff

        val lastKeyIndex =
          if (tiff.overviews.nonEmpty) {
            val lastKeyBounds = tiff.overviews.last._2.bounds match {
              case kb: KeyBounds[K] => kb
              case EmptyBounds => throw new Exception("Can't write an empty COG Layer.")
            }
            keyIndexMethod.createIndex(lastKeyBounds)
          }
          else keyIndexMethod.createIndex(keyBounds)

        val maxWidth = Index.digits(lastKeyIndex.toIndex(lastKeyIndex.keyBounds.maxKey))
        val keyPath = KeyPathGenerator(catalogPath, path, keyIndex, maxWidth)

        // collect bands information
        val samples: List[(Int, Elem)] = (0 until bandsCount).map { b =>
          vrt.simpleSource(s"s3://${keyPath(key)}.tiff", b)(geoTiff.cols, geoTiff.rows)(geoTiff.extent)
        }.toList
        samples.foreach(samplesAccumulator.add)

        tiff.geoTiff.write(keyPath(key), true)
      }
    }

    // at least one vrt builder should be avail, otherwise the dataset is empty
    val vrt = vrts.value.get(0)
    vrt.fromSimpleSources(samplesAccumulator.value.asScala.toList)
  }
}

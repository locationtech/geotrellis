/*
 * Copyright 2017 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.store.cog

import geotrellis.tiling._
import geotrellis.raster.{CellGrid, GridBounds, MultibandTile, RasterExtent, Tile}
import geotrellis.raster.crop._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.raster.io.geotiff.reader.TiffTagsReader
import geotrellis.layers._
import geotrellis.layers.cog._
import geotrellis.layers.index.{Index, IndexRanges, KeyIndex, MergeQueue}
import geotrellis.layers.util.IOUtils
import geotrellis.spark._
import geotrellis.spark.store._
import geotrellis.spark.util.KryoWrapper
import geotrellis.util._


import spray.json._

import org.apache.spark.rdd._
import org.apache.spark.SparkContext

import java.net.URI
import java.util.ServiceLoader


import scala.reflect._

abstract class COGLayerReader[ID] extends Serializable {

  val attributeStore: AttributeStore

  def defaultNumPartitions: Int

  protected def pathExists(path: String): Boolean

  protected def fullPath(path: String): URI

  protected def getHeader(id: LayerId): LayerHeader

  protected def produceGetKeyPath(id: LayerId): (ZoomRange, Int) => BigInt => String

  /** read
    *
    * This function will read an RDD layer based on a query.
    *
    * @param id              The ID of the layer to be read
    * @param rasterQuery     The query that will specify the filter for this read.
    * @param numPartitions   The desired number of partitions in the resulting RDD.
    *
    * @tparam K              Type of RDD Key (ex: SpatialKey)
    * @tparam V              Type of RDD Value (ex: Tile or MultibandTile )
    */
  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: ID, rasterQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int): RDD[(K, V)] with Metadata[TileLayerMetadata[K]]

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: ID, rasterQuery: LayerQuery[K, TileLayerMetadata[K]]): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read(id, rasterQuery, defaultNumPartitions)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: ID, numPartitions: Int): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read(id, new LayerQuery[K, TileLayerMetadata[K]], numPartitions)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](id: ID): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
    read(id, defaultNumPartitions)

  /**
    *
    * This method will read in an RDD layer whose value will only contain the
    * desired bands in their given order. This value is represented as an: Array[Option[Tile]].
    * Where Some(tile) represents a single band and None represents a band that could not
    * be accessed.
    *
    * @param id              The ID of the layer to be read
    * @param targetBands     The desired set of bands the output layer should have.
    * @param rasterQuery     The query that will specify the filter for this read.
    * @param numPartitions   The desired number of partitions in the resulting RDD.
    *
    * @tparam K              Type of RDD Key (ex: SpatialKey)
    */
  def readSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](
    id: ID,
    targetBands: Seq[Int],
    rasterQuery: LayerQuery[K, TileLayerMetadata[K]],
    numPartitions: Int
  ): RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]]

  def readSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](
    id: ID,
    targetBands: Seq[Int],
    rasterQuery: LayerQuery[K, TileLayerMetadata[K]]
  ): RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]] =
    readSubsetBands(id, targetBands, rasterQuery, defaultNumPartitions)

  def readSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](id: ID, targetBands: Seq[Int], numPartitions: Int): RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]] =
    readSubsetBands(id, targetBands, new LayerQuery[K, TileLayerMetadata[K]], numPartitions)

  def readSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](id: ID, targetBands: Seq[Int]): RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]] =
    readSubsetBands(id, targetBands, defaultNumPartitions)

  // TODO: Have this return a COGLayerReader
  def reader[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ]: Reader[ID, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new Reader[ID, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] {
      def read(id: ID): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] =
        COGLayerReader.this.read[K, V](id)
    }

  def query[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](layerId: ID): BoundLayerQuery[K, TileLayerMetadata[K], RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, read(layerId, _))

  def query[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](layerId: ID, numPartitions: Int): BoundLayerQuery[K, TileLayerMetadata[K], RDD[(K, V)] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, read(layerId, _, numPartitions))

  def querySubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](layerId: ID, targetBands: Seq[Int]): BoundLayerQuery[K, TileLayerMetadata[K], RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, readSubsetBands(layerId, targetBands, _))

  def querySubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](layerId: ID, targetBands: Seq[Int], numPartitions: Int): BoundLayerQuery[K, TileLayerMetadata[K], RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]]] =
    new BoundLayerQuery(new LayerQuery, readSubsetBands(layerId, targetBands, _, numPartitions))

  private def crop[V <: CellGrid[Int]: GeoTiffReader: ClassTag](
    geoTiff: GeoTiff[V],
    gridBounds: Seq[GridBounds[Int]]
  ): Iterator[(GridBounds[Int], V)] =
    geoTiff.crop(gridBounds)

  private def produceCropBands(
    targetBands: Seq[Int]
  ): (GeoTiff[MultibandTile], Seq[GridBounds[Int]]) => Iterator[(GridBounds[Int], Array[Option[Tile]])] =
    (geoTiff: GeoTiff[MultibandTile], gridBounds: Seq[GridBounds[Int]]) => cropBands(geoTiff, gridBounds, targetBands)

  private def cropBands(
    geoTiff: GeoTiff[MultibandTile],
    gridBounds: Seq[GridBounds[Int]],
    bands: Seq[Int]
  ): Iterator[(GridBounds[Int], Array[Option[Tile]])] = {
    // We first must determine which bands are valid and which are not
    // before doing the crop in order to avoid band subsetting errors
    // and/or loading unneeded data.
    val targetBandsWithIndex: Array[(Int, Int)] =
      bands
        .zipWithIndex
        .filter { case (band, _) =>
          band >= 0 && band < geoTiff.bandCount
        }
        .toArray

    val (targetBands, targetBandsIndexes) = targetBandsWithIndex.unzip

    val croppedTilesWithGridBounds: Iterator[(GridBounds[Int], Array[Tile])] =
      geoTiff
        .tile
        .cropBands(gridBounds, targetBands)
        .map { case (k, v) => k -> v.bands.toArray }

    val croppedTilesWithBandIndexes: Iterator[(GridBounds[Int], Array[(Int, Tile)])] =
      croppedTilesWithGridBounds
        .map { case (k, v) => k -> targetBandsIndexes.zip(v) }

    croppedTilesWithBandIndexes.map { case (k, v) =>
      val returnedBands = Array.fill[Option[Tile]](bands.size)(None)
      for ((index, band) <- v) {
        returnedBands(index) = Some(band)
      }

      k -> returnedBands
    }
  }

  def baseReadAllBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag
  ](
    id: LayerId,
    tileQuery: LayerQuery[K, TileLayerMetadata[K]],
    numPartitions: Int,
    defaultThreads: Int
  )(implicit sc: SparkContext,
             getByteReader: URI => ByteReader,
             idToLayerId: ID => LayerId
  ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {

    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      try {
        attributeStore.readMetadata[COGLayerStorageMetadata[K]](LayerId(id.name, 0))
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      }

    val metadata = cogLayerMetadata.tileLayerMetadata(id.zoom)

    val queryKeyBounds: Seq[KeyBounds[K]] = tileQuery(metadata)

    val readDefinitions: Seq[(ZoomRange, Seq[(SpatialKey, Int, GridBounds[Int], Seq[(GridBounds[Int], SpatialKey)])])] =
      cogLayerMetadata.getReadDefinitions(queryKeyBounds, id.zoom)

    readDefinitions.headOption.map(_._1) match {
      case Some(zoomRange) => {
        val baseKeyIndex = keyIndexes(zoomRange)

        baseRead[K, V, V](
          id = id,
          zoomRange = zoomRange,
          cogLayerMetadata = cogLayerMetadata,
          metadata = metadata,
          baseKeyIndex = baseKeyIndex,
          queryKeyBounds = queryKeyBounds,
          readDefinitions = readDefinitions.flatMap(_._2).groupBy(_._1),
          readGeoTiff = crop,
          numPartitions = numPartitions,
          defaultThreads = defaultThreads
        )
      }
      case None =>
        new ContextRDD(sc.parallelize(Seq()), metadata.setComponent[Bounds[K]](EmptyBounds))
    }
  }

  def baseReadSubsetBands[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag
  ](
    id: LayerId,
    targetBands: Seq[Int],
    tileQuery: LayerQuery[K, TileLayerMetadata[K]],
    numPartitions: Int,
    defaultThreads: Int
  )(implicit sc: SparkContext,
             getByteReader: URI => ByteReader,
             idToLayerId: ID => LayerId
  ): RDD[(K, Array[Option[Tile]])] with Metadata[TileLayerMetadata[K]] = {

    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      try {
        attributeStore.readMetadata[COGLayerStorageMetadata[K]](LayerId(id.name, 0))
      } catch {
        // to follow GeoTrellis Layer Readers logic
        case e: AttributeNotFoundError => throw new LayerNotFoundError(id).initCause(e)
      }

    val metadata = cogLayerMetadata.tileLayerMetadata(id.zoom)

    val queryKeyBounds: Seq[KeyBounds[K]] = tileQuery(metadata)

    val readDefinitions: Seq[(ZoomRange, Seq[(SpatialKey, Int, GridBounds[Int], Seq[(GridBounds[Int], SpatialKey)])])] =
      cogLayerMetadata.getReadDefinitions(queryKeyBounds, id.zoom)

    readDefinitions.headOption.map(_._1) match {
      case Some(zoomRange) => {
        val baseKeyIndex = keyIndexes(zoomRange)

        val rdd =
          baseRead[K, MultibandTile, Array[Option[Tile]]](
            id = id,
            zoomRange = zoomRange,
            cogLayerMetadata = cogLayerMetadata,
            metadata = metadata,
            baseKeyIndex = baseKeyIndex,
            queryKeyBounds = queryKeyBounds,
            readDefinitions = readDefinitions.flatMap(_._2).groupBy(_._1),
            readGeoTiff = produceCropBands(targetBands),
            numPartitions = numPartitions,
            defaultThreads = defaultThreads
          )

        new ContextRDD(rdd, metadata)
      }
      case None =>
        new ContextRDD(sc.parallelize(Seq()), metadata.setComponent[Bounds[K]](EmptyBounds))
    }
  }

  private def baseRead[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader: ClassTag,
    R
  ](
    id: LayerId,
    zoomRange: ZoomRange,
    cogLayerMetadata: COGLayerMetadata[K],
    metadata: TileLayerMetadata[K],
    baseKeyIndex: KeyIndex[K],
    queryKeyBounds: Seq[KeyBounds[K]],
    readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, GridBounds[Int], Seq[(GridBounds[Int], SpatialKey)])]],
    readGeoTiff: (GeoTiff[V], Seq[GridBounds[Int]]) => Iterator[(GridBounds[Int], R)],
    numPartitions: Int,
    defaultThreads: Int
  )(implicit sc: SparkContext,
             getByteReader: URI => ByteReader,
             idToLayerId: ID => LayerId
  ): RDD[(K, R)] with Metadata[TileLayerMetadata[K]] = {
    val getKeyPath = produceGetKeyPath(id)

    val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
    val keyPath: BigInt => String = getKeyPath(zoomRange, maxWidth)
    val decompose = (bounds: KeyBounds[K]) => baseKeyIndex.indexRanges(bounds)

    val targetLayout = cogLayerMetadata.layoutForZoom(zoomRange.minZoom)
    val sourceLayout = cogLayerMetadata.layoutForZoom(id.zoom)

    val baseKeyBounds = cogLayerMetadata.zoomRangeInfoFor(zoomRange.minZoom)._2

    val baseQueryKeyBounds: Seq[KeyBounds[K]] =
      queryKeyBounds
        .flatMap { qkb =>
          qkb.rekey(sourceLayout, targetLayout).intersect(baseKeyBounds) match {
            case EmptyBounds => None
            case kb: KeyBounds[K] => Some(kb)
          }
        }
        .distinct

    val ranges = if (baseQueryKeyBounds.length > 1)
      MergeQueue(baseQueryKeyBounds.flatMap(decompose))
    else
      baseQueryKeyBounds.flatMap(decompose)

    val bins = IndexRanges.bin(ranges, numPartitions)

    val rdd =
      if (baseQueryKeyBounds.isEmpty)
        sc.emptyRDD[(K, R)]
      else
        readLayer[K, V, R](
          bins = bins,
          keyPath = keyPath,
          readDefinitions = readDefinitions,
          readGeoTiff = readGeoTiff,
          threads = defaultThreads
        )

    new ContextRDD(rdd, metadata)
  }

  private def readLayer[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid[Int]: GeoTiffReader,
    R
  ](
     bins: Seq[Seq[(BigInt, BigInt)]],
     keyPath: BigInt => String, // keyPath
     readDefinitions: Map[SpatialKey, Seq[(SpatialKey, Int, GridBounds[Int], Seq[(GridBounds[Int], SpatialKey)])]],
     readGeoTiff: (GeoTiff[V], Seq[GridBounds[Int]]) => Iterator[(GridBounds[Int], R)],
     threads: Int
   )(implicit sc: SparkContext, getByteReader: URI => ByteReader): RDD[(K, R)] = {
    val kwFormat = KryoWrapper(implicitly[JsonFormat[K]])

    sc.parallelize(bins, bins.size)
      .mapPartitions { partition: Iterator[Seq[(BigInt, BigInt)]] =>
        val keyFormat = kwFormat.value

        partition flatMap { seq =>
          IOUtils.parJoin[K, R](seq.toIterator, threads) { index: BigInt =>
            if (!pathExists(keyPath(index))) Vector()
            else {
              val uri = fullPath(keyPath(index))
              val byteReader: ByteReader = uri
              val baseKey = {
                val keyTag = TiffTagsReader.read(byteReader).tags.headTags(GTKey)
                val decoded = if (keyTag.contains('&')) {
                  org.apache.commons.lang.StringEscapeUtils.unescapeHtml(keyTag)
                } else keyTag
                decoded.parseJson.convertTo[K](keyFormat)
              }

              readDefinitions
                .get(baseKey.getComponent[SpatialKey])
                .toVector
                .flatten
                .flatMap { case (spatialKey, overviewIndex, _, seq) =>
                  val key = baseKey.setComponent(spatialKey)
                  val tiff = GeoTiffReader[V].read(uri, streaming = true).getOverview(overviewIndex)
                  val map = seq.map { case (gb, sk) => gb -> key.setComponent(sk) }.toMap

                  readGeoTiff(tiff, map.keys.toSeq)
                    .flatMap { case (k, v) => map.get(k).map(i => i -> v) }
                    .toVector
                }
            }
          }
        }
      }
  }
}

object COGLayerReader {
  /**
    * Produce COGLayerReader instance based on URI description.
    * Find instances of [[COGLayerReaderProvider]] through Java SPI.
    */
  def apply(attributeStore: AttributeStore, layerReaderUri: URI)(implicit sc: SparkContext): COGLayerReader[LayerId] = {
    import scala.collection.JavaConverters._
    ServiceLoader.load(classOf[COGLayerReaderProvider])
      .iterator().asScala
      .find(_.canProcess(layerReaderUri))
      .getOrElse(throw new RuntimeException(s"Unable to find LayerReaderProvider for $layerReaderUri"))
      .layerReader(layerReaderUri, attributeStore, sc)
  }

  /**
    * Produce COGLayerReader instance based on URI description.
    * Find instances of [[COGLayerReaderProvider]] through Java SPI.
    */
  def apply(attributeStoreUri: URI, layerReaderUri: URI)(implicit sc: SparkContext): COGLayerReader[LayerId] =
    apply(attributeStore = AttributeStore(attributeStoreUri), layerReaderUri)

  /**
    * Produce COGLayerReader instance based on URI description.
    * Find instances of [[COGLayerReaderProvider]] through Java SPI.
    * Required [[AttributeStoreProvider]] instance will be found from the same URI.
    */
  def apply(uri: URI)(implicit sc: SparkContext): COGLayerReader[LayerId] =
    apply(attributeStoreUri = uri, layerReaderUri = uri)

  def apply(attributeStore: AttributeStore, layerReaderUri: String)(implicit sc: SparkContext): COGLayerReader[LayerId] =
    apply(attributeStore, new URI(layerReaderUri))

  def apply(attributeStoreUri: String, layerReaderUri: String)(implicit sc: SparkContext): COGLayerReader[LayerId] =
    apply(new URI(attributeStoreUri), new URI(layerReaderUri))

  def apply(uri: String)(implicit sc: SparkContext): COGLayerReader[LayerId] =
    apply(new URI(uri))

}

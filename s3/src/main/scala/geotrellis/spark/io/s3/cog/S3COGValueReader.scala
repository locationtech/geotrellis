/*
 * Copyright 2016 Azavea
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

package geotrellis.spark.io.s3.cog

import java.net.URI

import com.amazonaws.services.s3.model.AmazonS3Exception
import geotrellis.raster.CellGrid
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.s3._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs.KeyValueRecordCodec
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.tiling.LayoutLevel
import org.apache.commons.io.IOUtils
import spray.json._

import scala.reflect.ClassTag

class S3COGValueReader(
  val attributeStore: AttributeStore
) extends OverzoomingCOGValueReader {

  def s3Client: S3Client = S3Client.DEFAULT

  def reader[K: JsonFormat: ClassTag, V <: CellGrid: TiffMethods](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[S3COGLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)

    val baseLayerId = layerId.copy(zoom = header.zoomRanges._1)
    val baseHeader = attributeStore.readHeader[S3COGLayerHeader](baseLayerId)
    val baseKeyIndex = attributeStore.readKeyIndex[K](baseLayerId)

    val bucket = header.bucket
    val prefix = header.key

    val tiffMethods = implicitly[TiffMethods[V]]

    val overviewIndex = header.zoomRanges._2 - layerId.zoom - 1

    val layoutScheme = header.layoutScheme

    val LayoutLevel(_, baseLayout) = layoutScheme.levelForZoom(header.zoomRanges._1)
    val LayoutLevel(_, layout) = layoutScheme.levelForZoom(layerId.zoom)

    def read(key: K): V = {
      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val path = s"$prefix/${Index.encode(baseKeyIndex.toIndex(key), maxWidth)}"

      val is =
        try {
          val uri = new URI(s"s3://$bucket/$path.tiff")
          val tiff = tiffMethods.readTiff(uri, overviewIndex)
          val rgb = layout.mapTransform(tiff.extent)

          val gb = tiff.rasterExtent.gridBounds
          val getGridBounds = tiffMethods.getSegmentGridBounds(uri, overviewIndex)




          s3Client.getObject(header.bucket, path).getObjectContent
        } catch {
          case e: AmazonS3Exception if e.getStatusCode == 404 =>
            throw new ValueNotFoundError(key, layerId)
        }

      val bytes = IOUtils.toByteArray(is)
      //val recs = AvroEncoder.fromBinary(writerSchema, bytes)(KeyValueRecordCodec[K, V])

      /*recs
        .find { row => row._1 == key }
        .map { row => row._2 }
        .getOrElse(throw new ValueNotFoundError(key, layerId))*/

      null.asInstanceOf[V]
    }
  }
}



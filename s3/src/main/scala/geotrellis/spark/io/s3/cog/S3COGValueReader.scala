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

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index._
import geotrellis.spark.io.s3.S3Client
import geotrellis.spark.tiling.LayoutLevel
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.util._

import spray.json._
import com.amazonaws.services.s3.model.AmazonS3Exception

import scala.reflect.ClassTag
import java.net.URI

// global context only for test purposes, should be refactored
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.blocking
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class S3COGValueReader(
  val attributeStore: AttributeStore
) extends OverzoomingCOGValueReader {

  def s3Client: S3Client = S3Client.DEFAULT

  def reader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: TiffMethods: ? => TileMergeMethods[V]
  ](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[S3COGLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)

    val baseLayerId = layerId.copy(zoom = header.zoomRanges._1)

    val baseHeader = attributeStore.readHeader[S3COGLayerHeader](baseLayerId)
    val baseMetadata = attributeStore.readMetadata[TileLayerMetadata[K]](baseLayerId)
    val baseKeyIndex = attributeStore.readKeyIndex[K](baseLayerId)

    val bucket = header.bucket
    val prefix = header.key

    val tiffMethods = implicitly[TiffMethods[V]]
    val overviewIndex = header.zoomRanges._2 - layerId.zoom - 1

    val layoutScheme = header.layoutScheme

    val LayoutLevel(_, baseLayout) = layoutScheme.levelForZoom(header.zoomRanges._1)
    val LayoutLevel(_, layout) = layoutScheme.levelForZoom(layerId.zoom)

    // TODO: BUG!!!
    def populateKeys(thisKey: K): Set[K] = {
      val extent = baseMetadata.extent
      val sourceRe = RasterExtent(extent, layout.layoutCols, layout.layoutRows)
      val targetRe = RasterExtent(extent, baseLayout.layoutCols, baseLayout.layoutRows)

      val minSpatialKey = thisKey.getComponent[SpatialKey]
      val (minCol, minRow) = {
        val (x, y) = sourceRe.gridToMap(minSpatialKey.col, minSpatialKey.row)
        targetRe.mapToGrid(x, y)
      }

      Set(
        thisKey.setComponent(SpatialKey(math.max(minCol - 1, 0), math.max(minRow - 1, 0))),
        thisKey.setComponent(SpatialKey(math.max(minCol - 1, 0), minRow)),
        thisKey.setComponent(SpatialKey(minCol, math.max(minRow - 1, 0))),
        thisKey.setComponent(SpatialKey(minCol, minRow)),
        thisKey.setComponent(SpatialKey(minCol + 1, minRow + 1)),
        thisKey.setComponent(SpatialKey(minCol + 1, minRow)),
        thisKey.setComponent(SpatialKey(minCol, minRow + 1))
      )
    }

    def transformKey(thisKey: K): K = {
      val extent = thisKey.getComponent[SpatialKey].extent(layout)
      val spatialKey = baseLayout.mapTransform(extent.center)
      thisKey.setComponent(spatialKey)
    }

    def read(key: K): V = {
      val _s3Client = s3Client
      val baseKey = transformKey(key)

      val neighbourBaseKeys = populateKeys(key)

      val maxWidth = Index.digits(baseKeyIndex.toIndex(baseKeyIndex.keyBounds.maxKey))
      val path = (k: K) => s"$prefix/${Index.encode(baseKeyIndex.toIndex(k), maxWidth)}"

      println(s"$baseKey path(transformKey(key)): ${path(transformKey(key))}")

      try {
        val tiles: Set[Future[Option[V]]] =
          neighbourBaseKeys
            .map { k => Future { blocking {
              val uri = new URI(s"s3://$bucket/${path(k)}.tiff")

              println(s"baseKey: $baseKey")
              println(s"uri $k: $uri")

              if (_s3Client.doesObjectExist(bucket, s"${path(k)}.tiff")) {
                val tiff = tiffMethods.readTiff(uri, overviewIndex)
                val rgb = layout.mapTransform(tiff.extent)

                val gb = tiff.rasterExtent.gridBounds
                val getGridBounds = tiffMethods.getSegmentGridBounds(uri, overviewIndex)

                val tiffGridBounds = {
                  val spatialKey = key.getComponent[SpatialKey]
                  val minCol = (spatialKey.col - rgb.colMin) * layout.tileLayout.tileCols
                  val minRow = (spatialKey.row - rgb.rowMin) * layout.tileLayout.tileRows

                  if (minCol >= 0 && minRow >= 0 && minCol < tiff.cols && minRow < tiff.rows) {
                    val currentGb = getGridBounds(minCol, minRow)
                    gb.intersection(currentGb)
                  } else None
                }

                tiffGridBounds.map(tiffMethods.tileTiff(tiff, _))
              } else None
            } } }

        Await
          .result(
            Future
              .sequence(tiles)
              .map(_.flatten)
              .map(_.reduce(_ merge _)),
            Duration.Inf
          )
      } catch {
        case e: AmazonS3Exception if e.getStatusCode == 404 =>
          throw new ValueNotFoundError(key, layerId)
      }
    }
  }
}

object S3COGValueReader {
  def apply(attributeStore: AttributeStore): S3COGValueReader =
    new S3COGValueReader(attributeStore)
}

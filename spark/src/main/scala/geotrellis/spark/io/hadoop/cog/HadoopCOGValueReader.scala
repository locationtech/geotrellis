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

package geotrellis.spark.io.hadoop.cog

import java.io.File
import java.net.URI

import com.github.blemale.scaffeine.{Cache, Scaffeine}
import geotrellis.raster._
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro.AvroEncoder
import geotrellis.spark.io.cog._
import geotrellis.spark.io.file.KeyPathGenerator
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.spark.io.index._
import geotrellis.spark.tiling.LayoutLevel
import geotrellis.util._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{BigIntWritable, BytesWritable, MapFile}
import spray.json._

import scala.collection.immutable.Vector
import scala.concurrent.Future
import scala.reflect.ClassTag

class HadoopCOGValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  maxOpenFiles: Int = 16
) extends OverzoomingCOGValueReader {

  val readers: Cache[(LayerId, Path), MapFile.Reader] =
    Scaffeine()
      .recordStats()
      .maximumSize(maxOpenFiles.toLong)
      .removalListener[(LayerId, Path), MapFile.Reader] { case (_, v, _) => v.close() }
      .build[(LayerId, Path), MapFile.Reader]

  private def predicate(row: (Path, BigInt, BigInt), index: BigInt): Boolean =
    (index >= row._2) && ((index <= row._3) || (row._3 == -1))

  def reader[
    K: JsonFormat: SpatialComponent: ClassTag,
    V <: CellGrid: TiffMethods: ? => TileMergeMethods[V]
  ](layerId: LayerId): Reader[K, V] = new Reader[K, V] {
    val header = attributeStore.readHeader[HadoopCOGLayerHeader](layerId)
    val keyIndex = attributeStore.readKeyIndex[K](layerId)
    val writerSchema = attributeStore.readSchema(layerId)

    val baseLayerId = layerId.copy(zoom = header.zoomRanges._1)

    val baseHeader = attributeStore.readHeader[HadoopCOGLayerHeader](baseLayerId)
    val baseMetadata = attributeStore.readMetadata[TileLayerMetadata[K]](baseLayerId)
    val baseKeyIndex = attributeStore.readKeyIndex[K](baseLayerId)

    val path = header.path

    val tiffMethods = implicitly[TiffMethods[V]]
    val overviewIndex = header.zoomRanges._2 - layerId.zoom - 1

    val layoutScheme = header.layoutScheme

    val LayoutLevel(_, baseLayout) = layoutScheme.levelForZoom(header.zoomRanges._1)
    val LayoutLevel(_, layout) = layoutScheme.levelForZoom(layerId.zoom)

    val ranges: Vector[(Path, BigInt, BigInt)] =
      FilterMapFileInputFormat.layerRanges(header.path, conf)

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
      val baseKey = transformKey(key)
      val neighbourBaseKeys = populateKeys(key)

      val tiles =
        neighbourBaseKeys
          .flatMap { k =>
            val index: BigInt = keyIndex.toIndex(k)
            val valueWritable: BytesWritable =
              ranges
                .find(row => predicate(row, index))
                .map { case (path, _, _) =>
                  readers.get((layerId, path), _ => new MapFile.Reader(path, conf))
                }
                .getOrElse(throw new ValueNotFoundError(key, layerId))
                .get(new BigIntWritable(index.toByteArray), new BytesWritable())
                .asInstanceOf[BytesWritable]

            if (valueWritable == null) throw new ValueNotFoundError(key, layerId)
            val bytes = valueWritable.getBytes
            val tiff = tiffMethods.readTiff(bytes, overviewIndex)
            val rgb = layout.mapTransform(tiff.extent)

            val gb = tiff.rasterExtent.gridBounds
            val getGridBounds = tiffMethods.getSegmentGridBounds(bytes, overviewIndex)

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
          }

      tiles.reduce(_ merge _)
    }
  }
}



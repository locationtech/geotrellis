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

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.hadoop.formats.FilterMapFileInputFormat
import geotrellis.util._

import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.{BigIntWritable, BytesWritable, MapFile}
import spray.json._
import com.github.blemale.scaffeine.{Cache, Scaffeine}

import scala.collection.immutable.Vector
import scala.reflect.ClassTag

class HadoopCOGValueReader(
  val attributeStore: AttributeStore,
  conf: Configuration,
  catalogPath: Path,
  maxOpenFiles: Int = 16
) extends OverzoomingCOGValueReader {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri)

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
    V <: CellGrid: TiffMethods
  ](layerId: LayerId): Reader[K, V] = new Reader[K, V] {

    val COGLayerStorageMetadata(cogLayerMetadata, keyIndexes) =
      attributeStore.read[COGLayerStorageMetadata[K]](LayerId(layerId.name, 0), "cog_metadata")

    val tiffMethods: TiffMethods[V] = implicitly[TiffMethods[V]]

    val ranges: Vector[(Path, BigInt, BigInt)] =
      FilterMapFileInputFormat.layerRanges(catalogPath, conf)

    def read(key: K): V = {
      val (zoomRange, spatialKey, overviewIndex, gridBounds) =
        cogLayerMetadata.getReadDefinition(key.getComponent[SpatialKey], layerId.zoom)

      val baseKeyIndex = keyIndexes(zoomRange)

      val index = baseKeyIndex.toIndex(key.setComponent(spatialKey))

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

      tiffMethods.cropTiff(tiff, gridBounds)
    }
  }
}

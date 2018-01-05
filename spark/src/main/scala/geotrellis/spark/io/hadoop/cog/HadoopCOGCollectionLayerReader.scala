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
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.index.Index
import geotrellis.spark.io.hadoop.HdfsUtils
import geotrellis.util._

import com.typesafe.config.ConfigFactory
import spray.json.JsonFormat

import java.net.URI

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from HDFS.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGCollectionLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val conf: Configuration = new Configuration,
  val defaultThreads: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.collection.read")
)
  extends COGCollectionLayerReader[LayerId] with LazyLogging {

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, conf)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], indexFilterOnly: Boolean) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      (index: BigInt) =>
        s"${catalogPath.toString}/${id.name}/" +
        s"${zoomRange.minZoom}_${zoomRange.maxZoom}/" +
        s"${Index.encode(index, maxWidth)}.$Extension"

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      indexFilterOnly = indexFilterOnly,
      getKeyPath      = getKeyPath,
      pathExists      = { str => HdfsUtils.pathExists(new Path(s"hdfs://$str"), conf) },
      fullPath        = { path => new URI(s"hdfs://$path") },
      defaultThreads  = defaultThreads
    )
  }
}



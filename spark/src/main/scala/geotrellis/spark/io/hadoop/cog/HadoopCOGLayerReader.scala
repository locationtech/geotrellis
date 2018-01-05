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
import geotrellis.spark.io.file.KeyPathGenerator
import geotrellis.spark.io.hadoop.HdfsUtils
import geotrellis.util._

import com.typesafe.config.ConfigFactory
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import spray.json.JsonFormat

import java.net.URI

import scala.reflect.ClassTag

/**
 * Handles reading raster RDDs and their metadata from S3.
 *
 * @param attributeStore  AttributeStore that contains metadata for corresponding LayerId
 */
class HadoopCOGLayerReader(
  val attributeStore: AttributeStore,
  val catalogPath: String,
  val defaultThreads: Int = ConfigFactory.load().getThreads("geotrellis.hadoop.threads.rdd.read")
)(@transient implicit val sc: SparkContext) extends FilteringCOGLayerReader[LayerId] with LazyLogging {

  val defaultNumPartitions: Int = sc.defaultParallelism

  implicit def getByteReader(uri: URI): ByteReader = byteReader(uri, sc.hadoopConfiguration)

  def read[
    K: SpatialComponent: Boundable: JsonFormat: ClassTag,
    V <: CellGrid: TiffMethods: (? => TileMergeMethods[V]): ClassTag
  ](id: LayerId, tileQuery: LayerQuery[K, TileLayerMetadata[K]], numPartitions: Int, filterIndexOnly: Boolean) = {
    def getKeyPath(zoomRange: ZoomRange, maxWidth: Int): BigInt => String =
      KeyPathGenerator(catalogPath, s"${id.name}/${zoomRange.slug}", maxWidth) andThen (_ ++ s".$Extension")

    baseRead[K, V](
      id              = id,
      tileQuery       = tileQuery,
      numPartitions   = numPartitions,
      filterIndexOnly = filterIndexOnly,
      getKeyPath      = getKeyPath,
      pathExists      = { str => HdfsUtils.pathExists(new Path(str), sc.hadoopConfiguration) },
      fullPath        = { path => new URI(s"hdfs://$path") },
      defaultThreads  = defaultThreads
    )
  }
}

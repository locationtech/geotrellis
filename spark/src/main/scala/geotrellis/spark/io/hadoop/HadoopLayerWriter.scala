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

package geotrellis.spark.io.hadoop

import geotrellis.raster.{MultibandTile, Tile}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.reflect._

class HadoopLayerWriter(
  rootPath: Path,
  val attributeStore: AttributeStore,
  indexInterval: Int = 4
) extends LayerWriter[LayerId] {

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rdd: RDD[(K, V)] with Metadata[M], keyIndex: KeyIndex[K]): Unit = {
    val layerPath =
      try {
        new Path(rootPath,  s"${id.name}/${id.zoom}")
      } catch {
        case e: Exception =>
          throw new InvalidLayerIdError(id).initCause(e)
      }

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = layerPath
      )
    val metadata = rdd.metadata

    try {
      attributeStore.writeLayerAttributes(id, header, metadata, keyIndex, KeyValueRecordCodec[K, V].schema)
      HadoopRDDWriter.write[K, V](rdd, layerPath, keyIndex, indexInterval)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {
  def apply(rootPath: Path, attributeStore: AttributeStore): HadoopLayerWriter =
    new HadoopLayerWriter(
      rootPath = rootPath,
      attributeStore = attributeStore
    )

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerWriter =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore(rootPath)
    )
}

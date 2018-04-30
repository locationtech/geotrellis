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

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag

class HadoopLayerMover(
  rootPath: Path,
  val attributeStore: AttributeStore
)(implicit sc: SparkContext) extends LayerMover[LayerId] {

  override def move[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[HadoopLayerHeader, M, K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerMoveError(from, to).initCause(e)
    }
    val (newLayerRoot, newPath) = new Path(rootPath,  s"${to.name}") -> new Path(rootPath,  s"${to.name}/${to.zoom}")
    // new layer name root has to be created before layerId renaming
    HdfsUtils.ensurePathExists(newLayerRoot, sc.hadoopConfiguration)
    HdfsUtils.renamePath(new Path(header.path), newPath, sc.hadoopConfiguration)
    attributeStore.writeLayerAttributes(
      to, header.copy(path = newPath.toUri), metadata, keyIndex, writerSchema
    )
    attributeStore.delete(from)
    attributeStore.clearCache()
  }
}

object HadoopLayerMover {
  def apply(
    rootPath: Path,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): HadoopLayerMover =
    new HadoopLayerMover(rootPath, attributeStore)

  def apply(rootPath: Path)(implicit sc: SparkContext): HadoopLayerMover =
    apply(rootPath, HadoopAttributeStore(rootPath, new Configuration))

  def apply(attributeStore: HadoopAttributeStore)(implicit sc: SparkContext): HadoopLayerMover =
    apply(attributeStore.rootPath, attributeStore)
}

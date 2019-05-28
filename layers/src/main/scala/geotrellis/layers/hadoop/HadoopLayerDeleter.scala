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

package geotrellis.layers.hadoop

import geotrellis.layers.LayerId
import geotrellis.layers._

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import com.typesafe.scalalogging.LazyLogging


class HadoopLayerDeleter(
  val attributeStore: AttributeStore,
  conf: Configuration
) extends LazyLogging with LayerDeleter[LayerId] {
  def delete(id: LayerId): Unit = {
    try {
      val header = attributeStore.readHeader[HadoopLayerHeader](id)
      HdfsUtils.deletePath(new Path(header.path), conf)
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      attributeStore.delete(id)
    }
  }
}

object HadoopLayerDeleter {
  def apply(attributeStore: AttributeStore, conf: Configuration): HadoopLayerDeleter =
    new HadoopLayerDeleter(attributeStore, conf)

  def apply(rootPath: Path, conf: Configuration): HadoopLayerDeleter =
    apply(HadoopAttributeStore(rootPath, conf), conf)
}

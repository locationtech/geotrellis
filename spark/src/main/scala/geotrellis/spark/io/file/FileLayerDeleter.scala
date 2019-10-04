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

package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import java.io.File

class FileLayerDeleter(val attributeStore: FileAttributeStore) extends LayerDeleter[LayerId] {
  @transient protected lazy val logger = Logger(LoggerFactory.getLogger(getClass.getName))

  def delete(id: LayerId): Unit =
    try {
      val header = attributeStore.readHeader[FileLayerHeader](id)
      val sourceLayerPath = new File(attributeStore.catalogPath, header.path)
      sourceLayerPath.delete
    } catch {
      case e: AttributeNotFoundError =>
        logger.info(s"Metadata for $id was not found. Any associated layer data (if any) will require manual deletion")
        throw new LayerDeleteError(id).initCause(e)
    } finally {
      for((_, file) <- attributeStore.attributeFiles(id)) {
        file.delete()
      }
    }
}

object FileLayerDeleter {
  def apply(attributeStore: FileAttributeStore): FileLayerDeleter =
    new FileLayerDeleter(attributeStore)

  def apply(catalogPath: String): FileLayerDeleter =
    apply(FileAttributeStore(catalogPath))
}

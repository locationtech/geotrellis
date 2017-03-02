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
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.index._

import spray.json.JsonFormat
import org.apache.avro.Schema

import scala.reflect.ClassTag
import java.io.File

object FileLayerDeleter {
  def apply(attributeStore: FileAttributeStore): LayerDeleter[LayerId] =
    new LayerDeleter[LayerId] {
      def delete(layerId: LayerId): Unit = {
        // Read the metadata file out.
        val header =
          try {
            attributeStore.readHeader[FileLayerHeader](layerId)
          } catch {
            case e: AttributeNotFoundError =>
              throw new LayerNotFoundError(layerId).initCause(e)
          }

        // Delete the attributes
        for((_, file) <- attributeStore.attributeFiles(layerId)) {
          file.delete()
        }

        // Delete the elements
        val sourceLayerPath = new File(attributeStore.catalogPath, header.path)
        if(sourceLayerPath.exists()) {
          sourceLayerPath
            .listFiles()
            .foreach(_.delete())

          sourceLayerPath.delete
        }
      }
    }

  def apply(catalogPath: String): LayerDeleter[LayerId] =
    apply(FileAttributeStore(catalogPath))
}

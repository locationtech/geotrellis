/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.store.file

import geotrellis.layers.LayerId
import geotrellis.layers._
import geotrellis.layers.file.FileCollectionLayerProvider
import geotrellis.spark.store._

import org.apache.spark.SparkContext

import java.net.URI
import java.io.File


/**
 * Provides [[FileLayerReader]] instance for URI with `file` scheme.
 * The uri represents local path to catalog root.
 *  ex: `file:/tmp/catalog`
 */
class FileSparkLayerProvider extends FileCollectionLayerProvider with LayerReaderProvider with LayerWriterProvider {
  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    val file = new File(uri)
    new FileLayerReader(store, file.getCanonicalPath)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val file = new File(uri)
    new FileLayerWriter(store, file.getCanonicalPath)
  }
}

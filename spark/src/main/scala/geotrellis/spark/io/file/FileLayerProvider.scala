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

package geotrellis.spark.io.file

import geotrellis.spark._
import geotrellis.spark.io._
import org.apache.spark.SparkContext
import java.net.URI
import java.io.File

/**
 * Provides [[FileLayerReader]] instance for URI with `file` scheme.
 * The uri represents local path to catalog root.
 *  ex: `file:/tmp/catalog`
 */
class FileLayerProvider extends AttributeStoreProvider
    with LayerReaderProvider with LayerWriterProvider with ValueReaderProvider with CollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def attributeStore(uri: URI): AttributeStore = {
    val file = new File(uri)
    new FileAttributeStore(file.getCanonicalPath)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    val file = new File(uri)
    new FileLayerReader(store, file.getCanonicalPath)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val file = new File(uri)
    new FileLayerWriter(store, file.getCanonicalPath)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileValueReader(store, catalogPath)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): CollectionLayerReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCollectionLayerReader(store, catalogPath)
  }
}

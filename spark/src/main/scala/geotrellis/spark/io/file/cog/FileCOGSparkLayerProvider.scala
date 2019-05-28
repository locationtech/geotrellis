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

package geotrellis.spark.io.file.cog

import geotrellis.layers.LayerId
import geotrellis.layers.cog.{COGCollectionLayerReader, COGCollectionLayerReaderProvider, COGValueReader}
import geotrellis.layers.{AttributeStore, AttributeStoreProvider}
import geotrellis.layers.file.FileAttributeStore
import geotrellis.layers.file.cog.{FileCOGCollectionLayerReader, FileCOGValueReader}
import geotrellis.spark._
import geotrellis.spark.io.cog._
import geotrellis.spark.io.file._

import org.apache.spark.SparkContext

import java.net.URI
import java.io.File


/**
 * Provides [[FileLayerReader]] instance for URI with `file` scheme.
 * The uri represents local path to catalog root.
 *  ex: `file:/tmp/catalog`
 */
class FileCOGSparkLayerProvider extends COGLayerReaderProvider with COGLayerWriterProvider with COGCollectionLayerReaderProvider {

  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => if (str.toLowerCase == "file") true else false
    case null => true // assume that the user is passing in the path to the catalog
  }

  def attributeStore(uri: URI): AttributeStore = {
    val file = new File(uri)
    new FileAttributeStore(file.getCanonicalPath)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): COGLayerReader[LayerId] = {
    val file = new File(uri)
    new FileCOGLayerReader(store, file.getCanonicalPath)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): COGLayerWriter = {
    val file = new File(uri)
    new FileCOGLayerWriter(store, file.getCanonicalPath)
  }

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCOGValueReader(store, catalogPath)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore): COGCollectionLayerReader[LayerId] = {
    val catalogPath = new File(uri).getCanonicalPath
    new FileCOGCollectionLayerReader(store, catalogPath)
  }
}

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

package geotrellis.store.hadoop

import geotrellis.store._
import geotrellis.store.hadoop.util.HdfsUtils
import geotrellis.util.UriUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

import java.net.URI

class HadoopCollectionLayerProvider extends AttributeStoreProvider with ValueReaderProvider with CollectionLayerReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => SCHEMES contains str.toLowerCase
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val path = new Path(HdfsUtils.trim(uri))
    val conf = new Configuration()
    HadoopAttributeStore(path, conf)
  }

  def valueReader(uri: URI, store: AttributeStore): ValueReader[LayerId] = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val params = UriUtils.getParams(_uri)
    val conf = new Configuration()
    val maxOpenFiles = params.getOrElse("maxOpenFiles", "16").toInt
    new HadoopValueReader(store, conf, maxOpenFiles)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore) = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val conf = new Configuration()
    HadoopCollectionLayerReader(path, conf)
  }
}

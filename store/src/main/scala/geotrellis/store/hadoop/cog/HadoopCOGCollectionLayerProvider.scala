/*
 * Copyright 2019 Azavea
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

package geotrellis.store.hadoop.cog

import geotrellis.store.LayerId
import geotrellis.store.{AttributeStore, AttributeStoreProvider}
import geotrellis.store.cog.{COGCollectionLayerReaderProvider, COGValueReader, COGValueReaderProvider}
import geotrellis.store.hadoop.{HadoopAttributeStore, SCHEMES}
import geotrellis.store.hadoop.util.HdfsUtils
import geotrellis.util.UriUtils

import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration

import java.net.URI


/**
 * Provides [[HadoopAttributeStore]] instance for URI with `hdfs`, `hdfs+file`, `s3n`, `s3a`, `wasb` and `wasbs` schemes.
 * The uri represents Hadoop [[Path]] of catalog root.
 * `wasb` and `wasbs` provide support for the Hadoop Azure connector. Additional
 * configuration is required for this.
 * This Provider intentinally does not handle the `s3` scheme because the Hadoop implemintation is poor.
 * That support is provided by [[HadoopAttributeStore]]
 */
class HadoopCOGCollectionLayerProvider extends AttributeStoreProvider with COGValueReaderProvider with COGCollectionLayerReaderProvider {
  def canProcess(uri: URI): Boolean = uri.getScheme match {
    case str: String => SCHEMES contains str.toLowerCase
    case null => false
  }

  def attributeStore(uri: URI): AttributeStore = {
    val path = new Path(HdfsUtils.trim(uri))
    val conf = new Configuration()
    HadoopAttributeStore(path, conf)
  }

  def valueReader(uri: URI, store: AttributeStore): COGValueReader[LayerId] = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val params = UriUtils.getParams(_uri)
    val conf = new Configuration()
    new HadoopCOGValueReader(store, conf)
  }

  def collectionLayerReader(uri: URI, store: AttributeStore) = {
    val _uri = HdfsUtils.trim(uri)
    val path = new Path(_uri)
    val conf = new Configuration()
    HadoopCOGCollectionLayerReader(path, conf)
  }
}

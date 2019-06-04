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

package geotrellis.spark.store.hadoop.cog

import java.net.URI

import geotrellis.layers.LayerId
import geotrellis.layers.AttributeStore
import geotrellis.layers.hadoop.cog.HadoopCOGCollectionLayerProvider
import geotrellis.spark.store.cog._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext


/**
 * Provides [[HadoopAttributeStore]] instance for URI with `hdfs`, `hdfs+file`, `s3n`, `s3a`, `wasb` and `wasbs` schemes.
 * The uri represents Hadoop [[Path]] of catalog root.
 * `wasb` and `wasbs` provide support for the Hadoop Azure connector. Additional
 * configuration is required for this.
 * This Provider intentinally does not handle the `s3` scheme because the Hadoop implemintation is poor.
 * That support is provided by [[HadoopAttributeStore]]
 */
class HadoopCOGSparkLayerProvider extends HadoopCOGCollectionLayerProvider
    with COGLayerReaderProvider with COGLayerWriterProvider {

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): COGLayerReader[LayerId] = {
    // don't need uri because HadoopLayerHeader contains full path of the layer
    new HadoopCOGLayerReader(store)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): COGLayerWriter = {
    val _uri = trim(uri)
    val path = new Path(_uri)
    new HadoopCOGLayerWriter(path.toString, store)
  }
}

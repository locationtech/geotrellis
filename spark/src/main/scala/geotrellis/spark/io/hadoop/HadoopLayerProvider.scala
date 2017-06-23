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

package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.util.UriUtils
import org.apache.hadoop.fs.Path
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkContext
import java.net.URI

/**
 * Provides [[HadoopAttributeStore]] instance for URI with `hdfs`, `file`, `s3n`, and `s3a` schemes.
 * The uri represents Hadoop [[Path]] of catalog root.
 * This Provider intentinally does not handle the `s3` scheme because the Hadoop implemintation is poor.
 * That support is provided by [[S3Attributestore]]
 */
class HadoopLayerProvider extends AttributeStoreProvider
    with LayerReaderProvider with LayerWriterProvider {
  val schemes: Array[String] = Array("hdfs", "file", "s3n", "s3a")

  def canProcess(uri: URI): Boolean = schemes contains uri.getScheme.toLowerCase

  def attributeStore(uri: URI): AttributeStore = {
    val conf = new Configuration()
    new HadoopAttributeStore(new Path(uri), conf)
  }

  def layerReader(uri: URI, store: AttributeStore, sc: SparkContext): FilteringLayerReader[LayerId] = {
    // don't need uri because HadoopLayerHeader contains full path of the layer
    new HadoopLayerReader(store)(sc)
  }

  def layerWriter(uri: URI, store: AttributeStore): LayerWriter[LayerId] = {
    val path = new Path(uri)
    val params = UriUtils.getParams(uri)
    val interval = params.getOrElse("interval", "4").toInt
    new HadoopLayerWriter(path, store, interval)
  }
}

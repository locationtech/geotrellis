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

package geotrellis.spark.io.hadoop

import geotrellis.tiling.SpatialKey
import geotrellis.spark.render._
import java.net.URI

import geotrellis.layers.LayerId
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.spark.rdd.RDD


class SaveBytesToHadoopMethods[K](rdd: RDD[(K, Array[Byte])]) {

  /** Saves to Hadoop FileSystem, returns a count of records saved.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def saveToHadoop(keyToUri: K => String): Long =
    SaveToHadoop(rdd, keyToUri)

  /** Sets up saving to Hadoop, but returns an RDD so that writes can be chained.
    *
    * @param scheme    URI scheme, used to get a hadoop FileSystem object
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def setupSaveToHadoop(keyToUri: K => String): RDD[(K, Array[Byte])] =
    SaveToHadoop.setup(rdd, keyToUri)
}

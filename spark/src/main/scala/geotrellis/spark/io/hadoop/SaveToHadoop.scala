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

import scala.collection.concurrent.TrieMap

object SaveToHadoop {
  /**
    * @param id           A Layer ID
    * @param pathTemplate The template used to convert a Layer ID and a SpatialKey into a Hadoop URI
    *
    * @return             A functon which takes a spatial key and returns a Hadoop URI
    */
  def spatialKeyToPath(id: LayerId, pathTemplate: String): (SpatialKey => String) = {
    // Return λ
    { key =>
      pathTemplate
        .replace("{x}", key.col.toString)
        .replace("{y}", key.row.toString)
        .replace("{z}", id.zoom.toString)
        .replace("{name}", id.name)
    }
  }


  /** Saves records from an iterator and returns them unchanged.
    *
    * @param  recs      Key, Value records to be saved
    * @param  keyToUri  A function from K (a key) to Hadoop URI
    * @param  toBytes   A function from record to array of bytes
    * @param  conf      Hadoop Configuration to used to get FileSystem
    */
  def saveIterator[K, V](
    recs: Iterator[(K, V)],
    keyToUri: K => String,
    conf: Configuration
  )(toBytes: (K, V) => Array[Byte]): Iterator[(K, V)] = {
    val fsCache = TrieMap.empty[String, FileSystem]

    for ( row @ (key, data) <- recs ) yield {
      val path = keyToUri(key)
      val uri = new URI(path)
      val fs = fsCache.getOrElseUpdate(
        uri.getScheme,
        FileSystem.get(uri, conf))
      val out = fs.create(new Path(path))
      try { out.write(toBytes(key, data)) }
      finally { out.close() }
      row
    }
  }

  /**
    * Sets up saving to Hadoop, but returns an RDD so that writes can
    * be chained.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def setup[K](
    rdd: RDD[(K, Array[Byte])],
    keyToUri: K => String
  ): RDD[(K, Array[Byte])] = {
    rdd.mapPartitions { partition =>
      saveIterator(partition, keyToUri, new Configuration){ (k, v) => v }
   }
  }

  /**
    * Sets up saving to Hadoop, but returns an RDD so that writes can
    * be chained.
    *
    * @param  keyToUri  A function from K (a key) to a Hadoop URI
    * @param  toBytes   A function from record to array of bytes
    */
  def setup[K, V](
    rdd: RDD[(K, V)],
    keyToUri: K => String,
    toBytes: (K, V) => Array[Byte]
  ): RDD[(K, V)] = {
    val conf = rdd.context.hadoopConfiguration
    rdd.mapPartitions { partition =>
      saveIterator(partition, keyToUri, new Configuration)(toBytes)
    }
  }

  /**
    * Saves to Hadoop FileSystem, returns an count of records saved.
    *
    * @param keyToUri A function from K (a key) to a Hadoop URI
    */
  def apply[K](
    rdd: RDD[(K, Array[Byte])],
    keyToUri: K => String
  ): Long =
    setup(rdd, keyToUri).count

  /**
    * Saves to Hadoop FileSystem, returns an count of records saved.
    *
    * @param  keyToUri  A function from K (a key) to a Hadoop URI
    * @param  toBytes   A function from record to array of bytes
    */
  def apply[K, V](
    rdd: RDD[(K, V)],
    keyToUri: K => String,
    toBytes: (K, V) => Array[Byte]
  ): Long =
    setup(rdd, keyToUri, toBytes).count
}

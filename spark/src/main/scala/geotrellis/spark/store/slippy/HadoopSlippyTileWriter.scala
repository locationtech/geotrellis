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

package geotrellis.spark.store.slippy

import geotrellis.tiling.SpatialKey
import geotrellis.spark._
import geotrellis.spark.store.hadoop._

import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.hadoop.fs.Path

import java.io.File

class HadoopSlippyTileWriter[T](uri: String, extension: String)(getBytes: (SpatialKey, T) => Array[Byte])(implicit sc: SparkContext) extends SlippyTileWriter[T] {
  def setupWrite(zoom: Int, rdd: RDD[(SpatialKey, T)]): RDD[(SpatialKey, T)] = {
    val lZoom = zoom
    val lUri = uri
    val lExtension = extension
    val scheme = new Path(System.getProperty("user.dir")).getFileSystem(sc.hadoopConfiguration).getScheme
    val keyToPath = { key: SpatialKey => new File(lUri, s"$lZoom/${key.col}/${key.row}.${lExtension}").getPath }
    rdd.setupSaveToHadoop(keyToPath)(getBytes)
  }
}
